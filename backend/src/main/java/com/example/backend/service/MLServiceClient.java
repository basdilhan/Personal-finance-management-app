package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Acts as the internal client that communicates securely with the Python FastAPI ML backend.
 */
@Service
public class MLServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Value("${ml.service.api.key}")
    private String mlServiceApiKey;

    public MLServiceClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(120000); // 120 seconds to allow Chronos ML to run on free CPU
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Helper to create headers with the secure API key.
     */
    private HttpHeaders getSecureHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", mlServiceApiKey);
        return headers;
    }


    /**
     * Call Step 6a Forecast endpoint.
     */
    public Double generateForecast(String userId, List<Double> historicalData) {
        String url = mlServiceUrl + "/api/ml/forecast";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("historical_data", historicalData);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, getSecureHeaders());

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null && response.containsKey("predicted_next_month_expense")) {
                Object val = response.get("predicted_next_month_expense");
                return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
            }
        } catch (Exception e) {
            System.err.println("Error calling ML Forecast endpoint: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Call Step 6c Cold Start K-Means endpoint.
     */
    public Map<String, Object> getColdStartProfile(String userId, int age, String incomeBracket, double savingsGoal) {
        String url = mlServiceUrl + "/api/ml/cold_start";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("age", age);
        requestBody.put("income_bracket", incomeBracket);
        requestBody.put("savings_goal", savingsGoal);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, getSecureHeaders());

        try {
            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            System.err.println("Error calling ML Cold Start endpoint: " + e.getMessage());
        }
        return new HashMap<>();
    }
}
