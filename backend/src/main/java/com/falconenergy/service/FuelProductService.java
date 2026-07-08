package com.falconenergy.service;

import com.falconenergy.dto.FuelProductRequest;
import com.falconenergy.dto.FuelProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FuelProductService {
    FuelProductResponse createProduct(FuelProductRequest request);
    FuelProductResponse getProductById(Long id);
    FuelProductResponse updateProduct(Long id, FuelProductRequest request);
    void deleteProduct(Long id);
    Page<FuelProductResponse> getAllProducts(String search, String status, Pageable pageable);
}
