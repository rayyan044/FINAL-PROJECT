package com.falconenergy.service;

import com.falconenergy.dto.PaymentAccountRequest;
import com.falconenergy.dto.PaymentAccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentAccountService {
    Page<PaymentAccountResponse> getAllPaymentAccounts(Pageable pageable);
    PaymentAccountResponse getPaymentAccountById(Long id);
    PaymentAccountResponse createPaymentAccount(PaymentAccountRequest request);
    PaymentAccountResponse updatePaymentAccount(Long id, PaymentAccountRequest request);
    void deletePaymentAccount(Long id);
    PaymentAccountResponse toggleStatus(Long id);
    List<PaymentAccountResponse> getActivePaymentAccounts();
}
