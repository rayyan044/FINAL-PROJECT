package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelTransactionResponse {
    private Long id;
    private String transactionNumber;
    private FuelProductResponse product;
    private BigDecimal quantity;
    private String transactionType;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
