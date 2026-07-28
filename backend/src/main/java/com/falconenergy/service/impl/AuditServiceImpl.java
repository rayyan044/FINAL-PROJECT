package com.falconenergy.service.impl;

import com.falconenergy.entity.AuditLog;
import com.falconenergy.repository.AuditLogRepository;
import com.falconenergy.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest httpServletRequest;

    @Override
    public void logAction(String username, String module, String action, String entityType, Long entityId, String oldValue, String newValue, String ipAddress) {
        String resolvedUsername = username;
        if (resolvedUsername == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                resolvedUsername = auth.getName();
            } else {
                resolvedUsername = "SYSTEM";
            }
        }

        String resolvedIp = ipAddress;
        if (resolvedIp == null) {
            try {
                resolvedIp = httpServletRequest.getHeader("X-Forwarded-For");
                if (resolvedIp == null || resolvedIp.isEmpty() || "unknown".equalsIgnoreCase(resolvedIp)) {
                    resolvedIp = httpServletRequest.getRemoteAddr();
                } else {
                    resolvedIp = resolvedIp.split(",")[0].trim();
                }
            } catch (Exception e) {
                resolvedIp = "127.0.0.1";
            }
        }

        AuditLog auditLog = AuditLog.builder()
                .username(resolvedUsername)
                .adminUsername(resolvedUsername) // compatibility
                .module(module != null ? module : "SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .previousValue(oldValue) // compatibility
                .newValue(newValue)
                .ipAddress(resolvedIp)
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now()) // compatibility
                .details(action + " performed on " + entityType + " (ID: " + entityId + ")") // compatibility
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log saved - User: {}, Action: {}, Module: {}", resolvedUsername, action, module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditHistory() {
        return auditLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getUserActivity(String username) {
        return auditLogRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
