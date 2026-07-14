package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.AuditLogResponse;
import com.falconenergy.entity.AuditLog;
import com.falconenergy.repository.AuditLogRepository;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/audit-logs", "/api/audit-logs"})
@PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<AuditLogResponse> responsePage = auditLogRepository.findAll(pageable)
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .adminId(log.getAdminId())
                        .adminUsername(log.getAdminUsername())
                        .action(log.getAction())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .affectedUsername(log.getAffectedUsername())
                        .ipAddress(log.getIpAddress())
                        .timestamp(log.getTimestamp())
                        .details(log.getDetails())
                        .previousValue(log.getPreviousValue())
                        .newValue(log.getNewValue())
                        .build()
                );

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", responsePage));
    }
}
