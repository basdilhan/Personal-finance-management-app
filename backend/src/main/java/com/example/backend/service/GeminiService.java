package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class GeminiService {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds connection timeout
        factory.setReadTimeout(10000);   // 10 seconds read timeout
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateChatResponse(String prompt) {
        String url;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("PLACEHOLDER_KEY")) {
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
        } else {
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;
        }

        // Build Gemini request body
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        // Adding a system instruction part if needed, but simple prompt works well:
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Try up to 4 times with exponential backoff (handles transient 503 errors from Google)
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                Map response = restTemplate.postForObject(url, request, Map.class);
                if (response != null && response.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (!parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
                break; // If no candidates but no error, don't retry
            } catch (Exception e) {
                System.err.println("Gemini API Error (attempt " + attempt + "): " + e.getMessage());
                if (attempt < 4) {
                    try { Thread.sleep(2000 * attempt); } catch (InterruptedException ignored) {}
                }
            }
        }
        
        return "I'm temporarily unable to process your request. The AI service is experiencing high demand — please try again in a moment.";
    }

    public String autoCategorize(String description) {
        String prompt = "Categorize this transaction description into exactly ONE of the following categories: " +
                        "'Food & Dining', 'Transportation', 'Mobile & Internet', 'Healthcare', 'Education', " +
                        "'Entertainment', 'Shopping', 'Groceries', 'Fuel', 'Other'.\n\n" +
                        "Description: " + description + "\n\n" +
                        "Return ONLY the exact category name with no other text.";
        
        String response = generateChatResponse(prompt);
        if (response.startsWith("I'm temporarily") || response.startsWith("Sorry,")) {
            return "Other";
        }
        return response.trim();
    }
}
