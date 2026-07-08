package com.falconenergy.service.impl;

import com.falconenergy.dto.InventoryRequest;
import com.falconenergy.dto.InventoryResponse;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.Inventory;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.InventoryMapper;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.InventoryRepository;
import com.falconenergy.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final FuelProductRepository fuelProductRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            FuelProductRepository fuelProductRepository,
            InventoryMapper inventoryMapper
    ) {
        this.inventoryRepository = inventoryRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {
        log.info("Recording manual inventory entry for product id: {} on date: {}", request.getProductId(), request.getRecordDate());
        FuelProduct product = fuelProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getProductId()));

        LocalDate date = request.getRecordDate();
        Inventory inventory = inventoryRepository.findByProductIdAndRecordDate(product.getId(), date)
                .orElseGet(() -> inventoryMapper.toEntity(request));

        if (inventory.getId() != null) {
            // Update fields if editing
            inventory.setOpeningStock(request.getOpeningStock());
            inventory.setReceivedStock(request.getReceivedStock());
            inventory.setIssuedStock(request.getIssuedStock());
        } else {
            inventory.setProduct(product);
        }

        // closingStock = openingStock + receivedStock - issuedStock
        BigDecimal closing = inventory.getOpeningStock().add(inventory.getReceivedStock()).subtract(inventory.getIssuedStock());
        inventory.setClosingStock(closing);

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory saved successfully with closing stock: {}", closing);
        return inventoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id: " + id));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventoryRecords(Long productId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Inventory> spec = Specification.where(null);

        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("recordDate"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("recordDate"), endDate));
        }

        return inventoryRepository.findAll(spec, pageable).map(inventoryMapper::toResponse);
    }
}
