package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerPortalOrderRequest {
    @NotNull private Long productId;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal quantity;
    private LocalDateTime deliveryDate;
    private String deliveryAddress;
    private String locationLandmark;
    private String paymentMethod;
    private String notes;
    private String destination;
}
