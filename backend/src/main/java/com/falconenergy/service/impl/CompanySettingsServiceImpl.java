package com.falconenergy.service.impl;

import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.dto.CompanySettingsResponse;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.mapper.CompanySettingsMapper;
import com.falconenergy.repository.CompanySettingsRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.CompanySettingsService;
import com.falconenergy.exception.BadRequestException;
import java.math.BigDecimal;
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
        validateDepotLocation(request);
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

    /** A partial depot is unsafe: new mapped orders must always have a real origin. */
    void validateDepotLocation(CompanySettingsRequest request) {
        boolean hasDepotValue = hasText(request.getDepotName()) || hasText(request.getDepotAddress())
                || request.getDepotLatitude() != null || request.getDepotLongitude() != null;
        if (!hasDepotValue) return; // Company details may still be saved before a depot has been configured.
        if (!hasText(request.getDepotName()) || !hasText(request.getDepotAddress())
                || request.getDepotLatitude() == null || request.getDepotLongitude() == null) {
            throw new BadRequestException("Depot name, selected address, latitude, and longitude are required.");
        }
        if (request.getDepotLatitude().compareTo(new BigDecimal("-90")) < 0 || request.getDepotLatitude().compareTo(new BigDecimal("90")) > 0) {
            throw new BadRequestException("Depot latitude must be between -90 and 90.");
        }
        if (request.getDepotLongitude().compareTo(new BigDecimal("-180")) < 0 || request.getDepotLongitude().compareTo(new BigDecimal("180")) > 0) {
            throw new BadRequestException("Depot longitude must be between -180 and 180.");
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
