package com.falconenergy.service.impl;

import com.falconenergy.entity.AuditLog;
import com.falconenergy.entity.User;
import com.falconenergy.repository.AuditLogRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;

    public AuditLogServiceImpl(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            HttpServletRequest httpServletRequest
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String affectedUsername, String details) {
        String adminUsername = "SYSTEM";
        Long adminId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            final String usernameToFind = authentication.getName();
            adminUsername = usernameToFind;
            // Try to resolve admin ID
            User adminUser = userRepository.findByEmail(usernameToFind)
                    .or(() -> userRepository.findByUsername(usernameToFind))
                    .orElse(null);
            if (adminUser != null) {
                adminId = adminUser.getId();
            }
        }

        // Retrieve client IP address
        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = httpServletRequest.getRemoteAddr();
        } else {
            // Take the first IP if forwarded through multiple proxies
            ipAddress = ipAddress.split(",")[0].trim();
        }

        AuditLog log = AuditLog.builder()
                .adminId(adminId)
                .adminUsername(adminUsername)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .affectedUsername(affectedUsername)
                .ipAddress(ipAddress)
                .details(details)
                .build();

        auditLogRepository.save(log);
    }
}
