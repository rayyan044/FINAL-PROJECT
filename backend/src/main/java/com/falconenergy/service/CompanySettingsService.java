package com.falconenergy.service;

import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.dto.CompanySettingsResponse;

public interface CompanySettingsService {
    CompanySettingsResponse getCompanySettings();
    CompanySettingsResponse updateCompanySettings(CompanySettingsRequest request);
}
