package com.falconenergy.service;
import com.falconenergy.dto.*; import com.falconenergy.entity.FuelProduct; import java.math.BigDecimal; import java.util.List;
public interface TransportPriceRangeService { List<TransportPriceRangeResponse> all(); TransportPriceRangeResponse create(TransportPriceRangeRequest r); TransportPriceRangeResponse update(Long id,TransportPriceRangeRequest r); TransportPriceRangeResponse toggle(Long id); void delete(Long id); BigDecimal resolve(FuelProduct product, BigDecimal litres); }
