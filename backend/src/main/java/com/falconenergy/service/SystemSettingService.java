package com.falconenergy.service;

import com.falconenergy.entity.SystemSetting;
import java.util.List;

public interface SystemSettingService {
    String getSetting(String key);
    String getSetting(String key, String defaultValue);
    SystemSetting updateSetting(String key, String value);
    List<SystemSetting> getAllSettings();
}
