package com.falconenergy.service;

import com.falconenergy.entity.SystemSetting;
import java.util.List;
import java.util.Map;

public interface SystemSettingService {
    String getSetting(String key);
    String getSetting(String key, String defaultValue);
    SystemSetting updateSetting(String key, String value);
    List<SystemSetting> getAllSettings();
}
