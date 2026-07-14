package com.falconenergy.service;

public interface AuditLogService {
    void log(String action, String entityType, Long entityId, String affectedUsername, String details);
    void log(String action, String entityType, Long entityId, String affectedUsername, String details, String previousValue, String newValue);
}
