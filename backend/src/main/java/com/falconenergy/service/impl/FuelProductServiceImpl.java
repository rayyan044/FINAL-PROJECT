package com.falconenergy.service.impl;

import com.falconenergy.dto.FuelProductRequest;
import com.falconenergy.dto.FuelProductResponse;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.FuelProductMapper;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.service.FuelProductService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class FuelProductServiceImpl implements FuelProductService {

    private final FuelProductRepository fuelProductRepository;
    private final FuelProductMapper fuelProductMapper;

    public FuelProductServiceImpl(FuelProductRepository fuelProductRepository, FuelProductMapper fuelProductMapper) {
        this.fuelProductRepository = fuelProductRepository;
        this.fuelProductMapper = fuelProductMapper;
    }

    @Override
    public FuelProductResponse createProduct(FuelProductRequest request) {
        log.info("Creating fuel product: {}", request.getProductName());
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isFinance = authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FINANCE"));
        if (isFinance) {
            throw new BadRequestException("Finance users are not permitted to add new fuel products.");
        }
        if (fuelProductRepository.existsByProductNameIgnoreCaseUnfiltered(request.getProductName())) {
            throw new DuplicateResourceException("Fuel product already exists: " + request.getProductName());
        }
        FuelProduct product = fuelProductMapper.toEntity(request);
        FuelProduct saved = fuelProductRepository.save(product);
        return fuelProductMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FuelProductResponse getProductById(Long id) {
        FuelProduct product = fuelProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + id));
        FuelProductResponse response = fuelProductMapper.toResponse(product);
        
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        
        if (isCustomer && response != null) {
            response.setAvailableQuantity(null);
        }
        return response;
    }

    @Override
    public FuelProductResponse updateProduct(Long id, FuelProductRequest request) {
        log.info("Updating fuel product with id: {}", id);
        FuelProduct product = fuelProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + id));

        // Get current authenticated user role
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        boolean isFinance = false;
        boolean isOperator = false;
        boolean isAdminOrManager = false;

        if (authentication != null && authentication.isAuthenticated()) {
            isFinance = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FINANCE"));
            isOperator = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERATIONS") || a.getAuthority().equals("ROLE_OPERATOR"));
            isAdminOrManager = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        }

        if (isFinance) {
            // Finance: Can only edit price per litre (unitPrice)
            log.info("Enforcing Finance restrictions on fuel product update.");
            if (request.getUnitPrice() != null) {
                product.setUnitPrice(request.getUnitPrice());
            }
        } else if (isOperator) {
            // Operator: Can only edit stock quantity and availability status
            log.info("Enforcing Operator restrictions on fuel product update.");
            if (request.getAvailableQuantity() != null) {
                product.setAvailableQuantity(request.getAvailableQuantity());
                
                // Automatic Availability Management
                // If Available Quantity > 0, Status = ACTIVE (Available)
                // If Available Quantity == 0, Status = UNAVAILABLE (Unavailable)
                String autoStatus = request.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0 ? "ACTIVE" : "UNAVAILABLE";
                if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                    // Manual override from operator
                    product.setStatus(request.getStatus().toUpperCase());
                } else {
                    product.setStatus(autoStatus);
                }
            } else if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                product.setStatus(request.getStatus().toUpperCase());
            }
        } else if (isAdminOrManager || authentication == null || !authentication.isAuthenticated()) {
            // Admin, Manager, or fallback (for bootstrap / tests if not authenticated)
            if (!product.getProductName().equalsIgnoreCase(request.getProductName()) &&
                    fuelProductRepository.existsByProductNameIgnoreCaseUnfilteredExcludeId(request.getProductName(), id)) {
                throw new DuplicateResourceException("Fuel product already exists: " + request.getProductName());
            }
            fuelProductMapper.updateEntityFromRequest(request, product);
        } else {
            throw new com.falconenergy.exception.BadRequestException("You do not have permission to update this product");
        }

        FuelProduct updated = fuelProductRepository.save(product);
        return fuelProductMapper.toResponse(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting fuel product with id: {}", id);
        FuelProduct product = fuelProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + id));
        fuelProductRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FuelProductResponse> getAllProducts(String search, String status, Pageable pageable) {
        Specification<FuelProduct> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("productName")), wildcard),
                    cb.like(cb.lower(root.get("fuelType")), wildcard)
            ));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        if (isCustomer) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.greaterThan(root.get("unitPrice"), BigDecimal.ZERO),
                    cb.or(
                        cb.equal(cb.lower(root.get("status")), "active"),
                        cb.equal(cb.lower(root.get("status")), "available")
                    )
            ));
        }

        Page<FuelProductResponse> response = fuelProductRepository.findAll(spec, pageable).map(fuelProductMapper::toResponse);
        if (isCustomer) {
            response.forEach(r -> {
                if (r != null) {
                    r.setAvailableQuantity(null);
                }
            });
        }
        return response;
    }
}
