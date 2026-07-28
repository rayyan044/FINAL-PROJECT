package com.falconenergy.service.impl;

import com.falconenergy.entity.SystemSetting;
import com.falconenergy.repository.SystemSettingRepository;
import com.falconenergy.service.AuditService;
import com.falconenergy.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return getSetting(key, null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    @Override
    public SystemSetting updateSetting(String key, String value) {
        log.info("Updating system setting: {} to {}", key, value);

        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> SystemSetting.builder()
                        .settingKey(key)
                        .description("Dynamic setting created programmatically")
                        .build());

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());

        SystemSetting saved = systemSettingRepository.save(setting);

        // Audit logging
        auditService.logAction(
                null,
                "SYSTEM_SETTINGS",
                "SETTING_UPDATED",
                "SYSTEM_SETTING",
                saved.getId(),
                key + ": " + oldValue,
                key + ": " + value,
                null
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }
}
