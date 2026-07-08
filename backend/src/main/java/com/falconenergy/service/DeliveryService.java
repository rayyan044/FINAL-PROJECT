package com.falconenergy.service;

import com.falconenergy.dto.DeliveryRequest;
import com.falconenergy.dto.DeliveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface DeliveryService {
    DeliveryResponse createDelivery(DeliveryRequest request);
    DeliveryResponse getDeliveryById(Long id);
    DeliveryResponse updateDelivery(Long id, DeliveryRequest request);
    DeliveryResponse updateDeliveryStatus(Long id, String status);
    void deleteDelivery(Long id);
    Page<DeliveryResponse> getAllDeliveries(String search, String status, Long driverId, Long vehicleId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
