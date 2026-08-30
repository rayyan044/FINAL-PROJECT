package com.falconenergy.service;

import com.falconenergy.dto.GeocodingLocationResponse;
import java.math.BigDecimal;
import java.util.List;

public interface GeocodingService {
    List<GeocodingLocationResponse> search(String query);

    GeocodingLocationResponse reverse(BigDecimal latitude, BigDecimal longitude);
}
