package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelOrderResponse {
    private Long id;
    private String orderNumber;
    private CustomerResponse customer;
    private FuelProductResponse product;
    private BigDecimal quantity;
    private BigDecimal amount;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private String orderStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal getAmount() {
        if (!"APPROVED".equalsIgnoreCase(orderStatus) && product != null && quantity != null) {
            if (product.getUnitPrice() != null) {
                return quantity.multiply(product.getUnitPrice());
            }
        }
        return amount;
    }

    private String driverName;
    private String driverPhone;
    private String driverEmail;
    private String locationGps;
    private String locationAddress;
    private String locationLandmark;
    private String vehicleType;
    private String emergencyLevel;
    private String paymentMethod;
    private String notes;
    private BigDecimal originalQuantity;
    private BigDecimal approvedQuantity;
    private String editReason;

    public String getCustomerName() {
        return customer != null ? customer.getCompanyName() : null;
    }

    public String getProductName() {
        return product != null ? product.getProductName() : null;
    }
}
