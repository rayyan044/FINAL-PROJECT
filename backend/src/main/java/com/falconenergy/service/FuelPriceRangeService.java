package com.falconenergy.service;

import com.falconenergy.dto.*;
import com.falconenergy.entity.FuelProduct;

import java.math.BigDecimal;
import java.util.List;

public interface FuelPriceRangeService {
    List<FuelPriceRangeResponse> getAll();
    FuelPriceRangeResponse create(FuelPriceRangeRequest request);
    FuelPriceRangeResponse update(Long id, FuelPriceRangeRequest request);
    FuelPriceRangeResponse toggleStatus(Long id);
    void delete(Long id);
    BigDecimal resolvePrice(FuelProduct product, BigDecimal quantity);
}
