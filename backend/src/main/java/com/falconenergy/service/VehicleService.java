package com.falconenergy.service;

import com.falconenergy.dto.VehicleRequest;
import com.falconenergy.dto.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleService {
    VehicleResponse createVehicle(VehicleRequest request);
    VehicleResponse getVehicleById(Long id);
    VehicleResponse updateVehicle(Long id, VehicleRequest request);
    void deleteVehicle(Long id);
    Page<VehicleResponse> getAllVehicles(String search, String status, Pageable pageable);
}
