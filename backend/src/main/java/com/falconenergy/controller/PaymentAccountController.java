package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.PaymentAccountRequest;
import com.falconenergy.dto.PaymentAccountResponse;
import com.falconenergy.service.PaymentAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/payment-accounts", "/api/payment-accounts"})
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    public PaymentAccountController(PaymentAccountService paymentAccountService) {
        this.paymentAccountService = paymentAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<Page<PaymentAccountResponse>>> getAllPaymentAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<PaymentAccountResponse> accounts = paymentAccountService.getAllPaymentAccounts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Payment accounts retrieved successfully", accounts));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'SALES_OFFICER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<PaymentAccountResponse>>> getActivePaymentAccounts() {
        List<PaymentAccountResponse> accounts = paymentAccountService.getActivePaymentAccounts();
        return ResponseEntity.ok(ApiResponse.success("Active payment accounts retrieved successfully", accounts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> getPaymentAccountById(@PathVariable Long id) {
        PaymentAccountResponse account = paymentAccountService.getPaymentAccountById(id);
        return ResponseEntity.ok(ApiResponse.success("Payment account retrieved successfully", account));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> createPaymentAccount(@RequestBody PaymentAccountRequest request) {
        PaymentAccountResponse account = paymentAccountService.createPaymentAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Payment account created successfully", account));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> updatePaymentAccount(@PathVariable Long id, @RequestBody PaymentAccountRequest request) {
        PaymentAccountResponse account = paymentAccountService.updatePaymentAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment account updated successfully", account));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<Void>> deletePaymentAccount(@PathVariable Long id) {
        paymentAccountService.deletePaymentAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Payment account deleted successfully", null));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> toggleStatus(@PathVariable Long id) {
        PaymentAccountResponse account = paymentAccountService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Payment account status toggled successfully", account));
    }
}
