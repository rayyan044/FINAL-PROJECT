package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.GeocodingLocationResponse;
import com.falconenergy.service.GeocodingService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/geocoding", "/api/geocoding"})
public class GeocodingController {
    private final GeocodingService geocoding;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GeocodingLocationResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Locations found", geocoding.search(q)));
    }

    @GetMapping("/reverse")
    public ResponseEntity<ApiResponse<GeocodingLocationResponse>> reverse(
            @RequestParam BigDecimal latitude, @RequestParam BigDecimal longitude) {
        return ResponseEntity.ok(ApiResponse.success("Location found", geocoding.reverse(latitude, longitude)));
    }
}
