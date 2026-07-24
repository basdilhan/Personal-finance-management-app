package com.example.backend.service;

import com.example.backend.entity.AuditLogEntity;
import com.example.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(String userId, String entityType, String action, String entityId, String details) {
        if (userId == null || userId.isEmpty()) return;
        AuditLogEntity log = new AuditLogEntity(userId, entityType, action, entityId, details);
        auditLogRepository.save(log);
    }
    
    public List<AuditLogEntity> getUserAuditLogs(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
