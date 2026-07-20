package com.example.backend.controller;

import com.example.backend.service.ReceiptParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ai")
public class AIApiController {

    private final ReceiptParserService receiptParserService;

    public AIApiController(ReceiptParserService receiptParserService) {
        this.receiptParserService = receiptParserService;
    }

    @PostMapping("/scan-receipt")
    public ResponseEntity<Map<String, Object>> scanReceipt(@RequestBody Map<String, String> body) {
        String base64Image = body.get("image");
        if (base64Image == null || base64Image.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No image provided");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = receiptParserService.parseReceipt(base64Image);
        return ResponseEntity.ok(result);
    }
}
