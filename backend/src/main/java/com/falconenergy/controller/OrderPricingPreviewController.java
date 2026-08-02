package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.service.FuelPriceRangeService;
import com.falconenergy.service.TransportPriceRangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController @RequestMapping({"/api/v1/order-pricing", "/api/order-pricing"}) @RequiredArgsConstructor
public class OrderPricingPreviewController {
 private final FuelProductRepository products; private final FuelPriceRangeService fuelPrices; private final TransportPriceRangeService transportPrices;
 @GetMapping("/preview") public ResponseEntity<ApiResponse<Map<String,Object>>> preview(@RequestParam Long productId,@RequestParam BigDecimal quantity){FuelProduct p=products.findById(productId).orElseThrow(()->new ResourceNotFoundException("Fuel product not found"));BigDecimal fuel=fuelPrices.resolvePrice(p,quantity), transport=transportPrices.resolve(p,quantity), fuelAmount=fuel.multiply(quantity);return ResponseEntity.ok(ApiResponse.success("Order price preview",Map.of("fuelPricePerLitre",fuel,"transportPrice",transport,"fuelAmount",fuelAmount,"totalAmount",fuelAmount.add(transport))));}
}
