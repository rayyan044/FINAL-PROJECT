package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.FuelTransactionRequest;
import com.falconenergy.dto.FuelTransactionResponse;
import com.falconenergy.service.FuelTransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping({"/api/v1/transactions", "/api/transactions"})
public class FuelTransactionController {

    private final FuelTransactionService fuelTransactionService;

    public FuelTransactionController(FuelTransactionService fuelTransactionService) {
        this.fuelTransactionService = fuelTransactionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<FuelTransactionResponse>> createTransaction(
            @Valid @RequestBody FuelTransactionRequest request
    ) {
        FuelTransactionResponse response = fuelTransactionService.createTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("Fuel transaction processed successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<FuelTransactionResponse>> getTransactionById(@PathVariable Long id) {
        FuelTransactionResponse response = fuelTransactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel transaction retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<FuelTransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "asc".equalsIgnoreCase(sort[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<FuelTransactionResponse> transactions = fuelTransactionService.getAllTransactions(search, type, productId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Fuel transactions retrieved successfully", transactions));
    }
}
