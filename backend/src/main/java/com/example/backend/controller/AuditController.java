package com.example.backend.controller;

import com.example.backend.entity.AuditLogEntity;
import com.example.backend.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogEntity> getAuditLogs(@RequestHeader("X-User-Id") String userId) {
        return auditService.getUserAuditLogs(userId);
    }
}
