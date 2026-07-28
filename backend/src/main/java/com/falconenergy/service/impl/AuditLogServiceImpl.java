package com.falconenergy.service.impl;

import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.AuditService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditService auditService;

    public AuditLogServiceImpl(@Lazy AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String affectedUsername, String details) {
        log(action, entityType, entityId, affectedUsername, details, null, null);
    }

    @Override
    public void log(String action, String entityType, Long entityId, String affectedUsername, String details, String previousValue, String newValue) {
        // Derive module from entityType
        String module = "SYSTEM";
        if (entityType != null) {
            module = entityType.toUpperCase();
        }
        auditService.logAction(null, module, action, entityType, entityId, previousValue, newValue, null);
    }
}
