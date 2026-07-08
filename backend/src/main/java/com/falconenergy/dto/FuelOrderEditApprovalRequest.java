package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelOrderEditApprovalRequest {
    @NotNull(message = "Approved quantity is required")
    @DecimalMin(value = "0.01", message = "Approved quantity must be positive")
    private BigDecimal approvedQuantity;

    private String reason;
}
