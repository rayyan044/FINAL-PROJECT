package com.falconenergy.service;

import com.falconenergy.dto.InventoryRequest;
import com.falconenergy.dto.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface InventoryService {
    InventoryResponse createOrUpdateInventory(InventoryRequest request);
    InventoryResponse getInventoryById(Long id);
    Page<InventoryResponse> getAllInventoryRecords(Long productId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
