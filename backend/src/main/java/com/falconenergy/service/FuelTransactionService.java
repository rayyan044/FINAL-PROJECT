package com.falconenergy.service;

import com.falconenergy.dto.FuelTransactionRequest;
import com.falconenergy.dto.FuelTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface FuelTransactionService {
    FuelTransactionResponse createTransaction(FuelTransactionRequest request);
    FuelTransactionResponse getTransactionById(Long id);
    Page<FuelTransactionResponse> getAllTransactions(String search, String type, Long productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
