package com.falconenergy.service.impl;

import com.falconenergy.dto.StorageTankRequest;
import com.falconenergy.dto.StorageTankResponse;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.StorageTank;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.StorageTankMapper;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.StorageTankRepository;
import com.falconenergy.service.StorageTankService;
import com.falconenergy.service.FuelTransactionService;
import com.falconenergy.dto.FuelTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
public class StorageTankServiceImpl implements StorageTankService {

    private final StorageTankRepository storageTankRepository;
    private final StorageTankMapper storageTankMapper;
    private final FuelProductRepository fuelProductRepository;
    private final FuelTransactionService fuelTransactionService;

    public StorageTankServiceImpl(
            StorageTankRepository storageTankRepository,
            StorageTankMapper storageTankMapper,
            FuelProductRepository fuelProductRepository,
            FuelTransactionService fuelTransactionService
    ) {
        this.storageTankRepository = storageTankRepository;
        this.storageTankMapper = storageTankMapper;
        this.fuelProductRepository = fuelProductRepository;
        this.fuelTransactionService = fuelTransactionService;
    }

    @Override
    public StorageTankResponse createTank(StorageTankRequest request) {
        log.info("Creating storage tank: {}", request.getTankName());
        if (storageTankRepository.existsByTankName(request.getTankName())) {
            throw new DuplicateResourceException("Tank name already exists: " + request.getTankName());
        }

        FuelProduct product = fuelProductRepository.findById(request.getFuelProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getFuelProductId()));

        if (request.getCurrentVolume().compareTo(request.getCapacity()) > 0) {
            throw new BadRequestException("Current volume cannot exceed tank capacity");
        }

        StorageTank tank = storageTankMapper.toEntity(request);
        tank.setFuelProduct(product);
        StorageTank saved = storageTankRepository.save(tank);
        return storageTankMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageTankResponse getTankById(Long id) {
        StorageTank tank = storageTankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage tank not found with id: " + id));
        return storageTankMapper.toResponse(tank);
    }

    @Override
    public StorageTankResponse updateTank(Long id, StorageTankRequest request) {
        log.info("Updating storage tank with id: {}", id);
        StorageTank tank = storageTankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage tank not found with id: " + id));

        if (!tank.getTankName().equals(request.getTankName()) &&
                storageTankRepository.existsByTankName(request.getTankName())) {
            throw new DuplicateResourceException("Tank name already exists: " + request.getTankName());
        }

        FuelProduct product = fuelProductRepository.findById(request.getFuelProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getFuelProductId()));

        if (request.getCurrentVolume().compareTo(request.getCapacity()) > 0) {
            throw new BadRequestException("Current volume cannot exceed tank capacity");
        }

        storageTankMapper.updateEntityFromRequest(request, tank);
        tank.setFuelProduct(product);
        StorageTank updated = storageTankRepository.save(tank);
        return storageTankMapper.toResponse(updated);
    }

    @Override
    public void deleteTank(Long id) {
        log.info("Deleting storage tank with id: {}", id);
        StorageTank tank = storageTankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage tank not found with id: " + id));
        storageTankRepository.delete(tank);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StorageTankResponse> getAllTanks(String search, Pageable pageable) {
        Specification<StorageTank> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tankName")), wildcard),
                    cb.like(cb.lower(root.get("location")), wildcard)
            ));
        }

        return storageTankRepository.findAll(spec, pageable).map(storageTankMapper::toResponse);
    }

    @Override
    public void adjustVolume(Long tankId, BigDecimal amount) {
        adjustVolume(tankId, amount, true);
    }

    @Override
    public void adjustVolume(Long tankId, BigDecimal amount, boolean isManual) {
        log.info("Adjusting volume of tank with id {} by {} (isManual={})", tankId, amount, isManual);
        StorageTank tank = storageTankRepository.findById(tankId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage tank not found with id: " + tankId));

        BigDecimal newVolume = tank.getCurrentVolume().add(amount);
        if (newVolume.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Insufficient volume in tank: " + tank.getTankName());
        }
        if (newVolume.compareTo(tank.getCapacity()) > 0) {
            throw new BadRequestException("Adjustment exceeds tank capacity: " + tank.getTankName());
        }

        tank.setCurrentVolume(newVolume);
        storageTankRepository.save(tank);
        log.info("Tank volume adjusted successfully. New volume: {}", newVolume);

        FuelProduct product = tank.getFuelProduct();
        if (product != null && isManual) {
            // Create transaction via FuelTransactionService to update product stock, daily sheet, and transactions
            com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                    .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .productId(product.getId())
                    .quantity(amount) // can be positive or negative
                    .transactionType("Adjustment")
                    .transactionDate(java.time.LocalDateTime.now())
                    .tankId(tank.getId())
                    .build();
            fuelTransactionService.createTransaction(txnReq);
            
            // Reload product and check availability status
            product = fuelProductRepository.findById(product.getId()).orElse(product);
            if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                product.setStatus("UNAVAILABLE");
                fuelProductRepository.save(product);
            } else if ("UNAVAILABLE".equals(product.getStatus())) {
                product.setStatus("ACTIVE");
                fuelProductRepository.save(product);
            }
        }
    }
}
