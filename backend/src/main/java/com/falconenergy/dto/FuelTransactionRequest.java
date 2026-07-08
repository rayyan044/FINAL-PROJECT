package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelTransactionRequest {

    @NotBlank(message = "Transaction number is required")
    private String transactionNumber;

    @NotNull(message = "Fuel product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    @NotBlank(message = "Transaction type is required")
    private String transactionType; // Purchase, Sale, Transfer, Adjustment

    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();

    private Long tankId;
}
