package com.falconenergy.controller;
import com.falconenergy.dto.*;
import com.falconenergy.entity.TruckPricing;
import com.falconenergy.repository.TruckPricingRepository;
import com.falconenergy.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
@RestController @RequestMapping({"/api/v1/truck-pricing", "/api/truck-pricing"}) @RequiredArgsConstructor
public class TruckPricingController {
 private final TruckPricingRepository repository;
 private final JdbcTemplate jdbcTemplate;
 private TruckPricingResponse map(TruckPricing p) { return TruckPricingResponse.builder().id(p.getId()).capacity(p.getCapacity()).fuelType(p.getFuelType()).transportPrice(p.getTransportPrice()).active(p.isActive()).build(); }
 @GetMapping public ResponseEntity<ApiResponse<List<TruckPricingResponse>>> list() { return ResponseEntity.ok(ApiResponse.success("Truck pricing retrieved", repository.findAll().stream().map(this::map).toList())); }
 @PostMapping @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FINANCE')") public ResponseEntity<ApiResponse<TruckPricingResponse>> create(@Valid @RequestBody TruckPricingRequest r) {
  String actor=SecurityContextHolder.getContext().getAuthentication().getName();
  Long id=jdbcTemplate.queryForObject("""
      INSERT INTO truck_pricing (capacity, fuel_type, transport_price, active, created_at, updated_at, created_by, updated_by, deleted)
      VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, FALSE)
      ON CONFLICT (capacity, fuel_type) DO UPDATE SET transport_price = EXCLUDED.transport_price, active = EXCLUDED.active,
        updated_at = CURRENT_TIMESTAMP, updated_by = EXCLUDED.updated_by
      RETURNING id
      """, Long.class, r.getCapacity(), r.getFuelType().trim().toUpperCase(), r.getTransportPrice(), r.isActive(), actor, actor);
  TruckPricing p=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Truck pricing could not be saved"));
  return ResponseEntity.ok(ApiResponse.success("Truck price saved",map(p)));
 }
 @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FINANCE')") public ResponseEntity<ApiResponse<TruckPricingResponse>> update(@PathVariable Long id,@Valid @RequestBody TruckPricingRequest r) { repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Truck pricing not found"));jdbcTemplate.update("UPDATE truck_pricing SET capacity=?, fuel_type=?, transport_price=?, active=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?", r.getCapacity(),r.getFuelType().trim().toUpperCase(),r.getTransportPrice(),r.isActive(),SecurityContextHolder.getContext().getAuthentication().getName(),id);return ResponseEntity.ok(ApiResponse.success("Truck price updated",map(repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Truck pricing not found"))))); }
}
