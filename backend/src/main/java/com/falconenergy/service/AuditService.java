package com.falconenergy.service;

import com.falconenergy.entity.AuditLog;
import java.util.List;

public interface AuditService {
    void logAction(String username, String module, String action, String entityType, Long entityId, String oldValue, String newValue, String ipAddress);
    List<AuditLog> getAuditHistory();
    List<AuditLog> getUserActivity(String username);
    List<AuditLog> getEntityHistory(String entityType, Long entityId);
}
