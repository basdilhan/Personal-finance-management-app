package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class ReceiptParserService {

    @Value("${gemini.api.key:PLACEHOLDER_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> parseReceipt(String base64Image) {
        String url;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("PLACEHOLDER_KEY")) {
            url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";
        } else {
            url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        }

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        
        Map<String, Object> textPart = new HashMap<>();
        String promptText = String.format(
            "You are an expert receipt parser. Extract the final total amount paid, transaction date, and infer a budget category from this receipt image. " +
            "Today's date is %s. If the receipt is missing the year, assume it's from the current year. " +
            "The category MUST be EXACTLY one of the following strings: 'Food & Dining', 'Transportation', 'Mobile & Internet', 'Healthcare', 'Education', 'Entertainment', 'Shopping', 'Groceries', 'Fuel', 'Other'. " +
            "Return ONLY a pure JSON object with exactly these keys: 'amount' (double), 'date' (string, strictly in YYYY-MM-DD format), 'category' (string). " +
            "Do not include any markdown formatting, backticks, or extra text.",
            java.time.LocalDate.now().toString()
        );
        textPart.put("text", promptText);
        parts.add(textPart);

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", base64Image);
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inlineData", inlineData);
        parts.add(imagePart);

        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            // Basic parsing of Gemini Response (assuming standard JSON structure)
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> contentResult = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, Object>> partsResult = (List<Map<String, Object>>) contentResult.get("parts");
                if (partsResult != null && !partsResult.isEmpty()) {
                    String jsonText = (String) partsResult.get(0).get("text");
                    jsonText = jsonText.replace("```json", "").replace("```", "").trim();
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return mapper.readValue(jsonText, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    } catch (Exception parseEx) {
                        Map<String, Object> res = new HashMap<>();
                        res.put("error", "Failed to parse JSON from AI: " + jsonText);
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI Error: " + e.getMessage() + ". Check if your API Key is valid.");
            return errorMap;
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("error", "Could not parse response from Gemini.");
        return fallback;
    }
}
