package com.falconenergy.controller;
import com.falconenergy.dto.*; import com.falconenergy.service.TransportPricingService; import jakarta.validation.Valid; import java.util.*; import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping({"/api/v1/transport-distance-rates","/api/transport-distance-rates"}) @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class TransportDistanceRateController { private final TransportPricingService service;
 @GetMapping public ResponseEntity<ApiResponse<List<TransportDistanceRateResponse>>> all(){return ResponseEntity.ok(ApiResponse.success("Distance transport rates retrieved",service.all()));}
 @PostMapping public ResponseEntity<ApiResponse<TransportDistanceRateResponse>> create(@Valid @RequestBody TransportDistanceRateRequest r){return ResponseEntity.ok(ApiResponse.success("Distance transport rate created",service.create(r)));}
 @PutMapping("/{id}") public ResponseEntity<ApiResponse<TransportDistanceRateResponse>> update(@PathVariable Long id,@Valid @RequestBody TransportDistanceRateRequest r){return ResponseEntity.ok(ApiResponse.success("Distance transport rate updated",service.update(id,r)));}
}
