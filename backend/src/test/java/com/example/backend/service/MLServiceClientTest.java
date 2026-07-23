package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MLServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MLServiceClient mlServiceClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(mlServiceClient, "mlServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(mlServiceClient, "mlServiceApiKey", "test-key");
    }

    @Test
    void testGetChatbotResponse_Fallback() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String response = mlServiceClient.getChatbotResponse("user1", "Hello", "Context");

        assertEquals("I'm currently unable to connect to the AI brain. Please try again later.", response);
    }

    @Test
    void testAutoCategorize_Fallback() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Timeout"));

        String response = mlServiceClient.autoCategorize("Amazon purchase");

        assertEquals("Other", response);
    }

    @Test
    void testGenerateForecast_Fallback() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("500 Internal Server Error"));

        Double response = mlServiceClient.generateForecast("user1", Arrays.asList(100.0, 200.0));

        assertEquals(0.0, response);
    }

    @Test
    void testGetColdStartProfile_Fallback() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Service Unavailable"));

        Map<String, Object> response = mlServiceClient.getColdStartProfile("user1", 25, "50k-100k", 5000.0);

        assertTrue(response.isEmpty());
    }
}
