package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminUsername;
    private String action;
    private String entityType;
    private Long entityId;
    private String affectedUsername;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String details;
}
