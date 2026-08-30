package com.falconenergy.service;
import com.falconenergy.dto.*;
import java.math.BigDecimal;
import java.util.List;
public interface TransportPricingService {
    BigDecimal resolveDistancePrice(BigDecimal distanceKm);
    List<TransportDistanceRateResponse> all();
    TransportDistanceRateResponse create(TransportDistanceRateRequest request);
    TransportDistanceRateResponse update(Long id, TransportDistanceRateRequest request);
}
