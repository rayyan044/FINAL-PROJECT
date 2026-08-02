package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.exception.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.FuelPriceRangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FuelPriceRangeServiceImpl implements FuelPriceRangeService {
    private final FuelPriceRangeRepository repository;
    private final FuelProductRepository fuelProductRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FuelPriceRangeResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public FuelPriceRangeResponse create(FuelPriceRangeRequest request) {
        validateRange(request, null);
        FuelPriceRange range = FuelPriceRange.builder()
                .fuelProduct(findProduct(request.getFuelProductId()))
                .minLitres(request.getMinLitres())
                .maxLitres(request.getMaxLitres())
                .pricePerLitre(request.getPricePerLitre())
                .effectiveDate(request.getEffectiveDate())
                .status(normalizeStatus(request.getStatus()))
                .build();
        setAuditFields(range, true);
        return toResponse(repository.save(range));
    }

    @Override
    public FuelPriceRangeResponse update(Long id, FuelPriceRangeRequest request) {
        FuelPriceRange range = findRange(id);
        validateRange(request, id);
        range.setFuelProduct(findProduct(request.getFuelProductId()));
        range.setMinLitres(request.getMinLitres());
        range.setMaxLitres(request.getMaxLitres());
        range.setPricePerLitre(request.getPricePerLitre());
        range.setEffectiveDate(request.getEffectiveDate());
        range.setStatus(normalizeStatus(request.getStatus()));
        setAuditFields(range, false);
        return toResponse(repository.save(range));
    }

    @Override
    public FuelPriceRangeResponse toggleStatus(Long id) {
        FuelPriceRange range = findRange(id);
        range.setStatus("ACTIVE".equals(range.getStatus()) ? "INACTIVE" : "ACTIVE");
        setAuditFields(range, false);
        return toResponse(repository.save(range));
    }

    @Override
    public void delete(Long id) {
        repository.delete(findRange(id));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolvePrice(FuelProduct product, BigDecimal quantity) {
        List<FuelPriceRange> matches = repository.findActiveMatches(product.getId(), quantity, LocalDate.now());
        if (matches.isEmpty()) {
            throw new BadRequestException("No active fuel price has been configured for the ordered litre quantity. Please contact Finance.");
        }
        if (matches.size() > 1) {
            throw new BadRequestException("More than one active fuel price matches this order quantity. Please contact Finance.");
        }
        return matches.getFirst().getPricePerLitre();
    }

    private void validateRange(FuelPriceRangeRequest request, Long excludeId) {
        if (request.getMinLitres().compareTo(request.getMaxLitres()) > 0) {
            throw new BadRequestException("Minimum litres cannot exceed maximum litres.");
        }
        findProduct(request.getFuelProductId());
        if (repository.existsOverlappingRange(request.getFuelProductId(), request.getMinLitres(), request.getMaxLitres(), excludeId)) {
            throw new BadRequestException("This litre range overlaps an existing price range for the selected fuel product.");
        }
    }

    private FuelProduct findProduct(Long id) {
        return fuelProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + id));
    }

    private FuelPriceRange findRange(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel price range not found with id: " + id));
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "ACTIVE" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new BadRequestException("Price range status must be ACTIVE or INACTIVE.");
        }
        return normalized;
    }

    private void setAuditFields(FuelPriceRange range, boolean creating) {
        String actor = SecurityContextHolder.getContext().getAuthentication() == null
                ? "system" : SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            range.setCreatedBy(actor);
            range.setCreatedAt(now);
        }
        range.setUpdatedBy(actor);
        range.setUpdatedAt(now);
    }

    private FuelPriceRangeResponse toResponse(FuelPriceRange range) {
        FuelProduct product = range.getFuelProduct();
        return FuelPriceRangeResponse.builder()
                .id(range.getId()).fuelProductId(product.getId()).fuelProductName(product.getProductName())
                .fuelType(product.getFuelType()).minLitres(range.getMinLitres()).maxLitres(range.getMaxLitres())
                .pricePerLitre(range.getPricePerLitre()).effectiveDate(range.getEffectiveDate()).status(range.getStatus())
                .createdBy(range.getCreatedBy()).createdAt(range.getCreatedAt()).updatedAt(range.getUpdatedAt()).build();
    }
}
