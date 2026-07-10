package com.falconenergy.service.impl;

import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.dto.CompanySettingsResponse;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.mapper.CompanySettingsMapper;
import com.falconenergy.repository.CompanySettingsRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.CompanySettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final CompanySettingsMapper companySettingsMapper;
    private final AuditLogService auditLogService;

    public CompanySettingsServiceImpl(
            CompanySettingsRepository companySettingsRepository,
            CompanySettingsMapper companySettingsMapper,
            AuditLogService auditLogService
    ) {
        this.companySettingsRepository = companySettingsRepository;
        this.companySettingsMapper = companySettingsMapper;
        this.auditLogService = auditLogService;
    }

    private String resolveCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "system";
    }

    private CompanySettings getOrCreateDefault() {
        return companySettingsRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            log.info("No company settings found. Creating default.");
            CompanySettings defaultSettings = CompanySettings.builder()
                    .companyName("FALCON ENERGY LIMITED")
                    .postalAddress("P.O. Box : 45431, 6th Floor, SALAMANDER TOWER")
                    .officeAddress("SAMORA AVENUE, DAR ES SALAAM")
                    .phoneNumber("+255 22 212 3456")
                    .email("info@falconenergy.co.tz")
                    .logo("assets/falcon-logo.png")
                    .signatoryName("AUTHORIZED SIGNATORY")
                    .signatoryTitle("FINANCE CONTROLLER")
                    .signatorySignature("assets/authorized-signature.png")
                    .build();
            return companySettingsRepository.save(defaultSettings);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettings() {
        log.info("Fetching company settings");
        return companySettingsMapper.toResponse(getOrCreateDefault());
    }

    @Override
    public CompanySettingsResponse updateCompanySettings(CompanySettingsRequest request) {
        log.info("Updating company settings");
        CompanySettings settings = getOrCreateDefault();
        companySettingsMapper.updateEntityFromRequest(request, settings);
        CompanySettings saved = companySettingsRepository.save(settings);

        auditLogService.log(
                "COMPANY_SETTINGS_UPDATED",
                "COMPANY_SETTINGS",
                saved.getId(),
                resolveCurrentUser(),
                "Company global settings updated by authorized officer."
        );

        return companySettingsMapper.toResponse(saved);
    }
}
