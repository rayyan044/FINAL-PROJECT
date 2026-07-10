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
public class FuelOrderRequest {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Fuel product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    private BigDecimal quantity;

    private LocalDateTime deliveryDate;

    private String driverName;
    private String driverPhone;
    private String driverEmail;
    private String locationGps;
    private String locationAddress;
    private String locationLandmark;
    private String emergencyLevel;
    private String paymentMethod;
    private String notes;
}
