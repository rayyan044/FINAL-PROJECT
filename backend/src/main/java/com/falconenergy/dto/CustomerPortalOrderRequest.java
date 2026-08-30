package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
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
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private BigDecimal deliveryLatitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private BigDecimal deliveryLongitude;
}
