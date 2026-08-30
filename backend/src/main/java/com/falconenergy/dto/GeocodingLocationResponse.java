package com.falconenergy.dto;

import java.math.BigDecimal;

public record GeocodingLocationResponse(String address, BigDecimal latitude, BigDecimal longitude) {
}
