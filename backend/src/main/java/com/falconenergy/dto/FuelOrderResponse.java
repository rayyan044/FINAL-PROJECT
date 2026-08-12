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
    private BigDecimal unitPrice;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private String orderStatus;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal getAmount() {
        return amount;
    }

    private String driverName;
    private String driverPhone;
    private String driverEmail;
    private String locationGps;
    private String locationAddress;
    private String locationLandmark;
    private String emergencyLevel;
    private String paymentMethod;
    private String notes;
    private BigDecimal originalQuantity;
    private BigDecimal approvedQuantity;
    private String editReason;
    private BigDecimal levies;
    private BigDecimal discount;
    private BigDecimal transportCharges;
    private BigDecimal deliveryCharges;
    private BigDecimal additionalCharges;
    private String deliveryMethod;
    private String incoterms;
    private String port;
    private String destination;
    private String logisticsInfo;
    
    private Long invoiceId;
    private String invoiceNumber;
    private String paymentStatus;

    public String getCustomerName() {
        if (customer != null && (
                "EMERGENCY".equalsIgnoreCase(customer.getCustomerCode()) ||
                "Stranded Drivers (Emergency Requests)".equalsIgnoreCase(customer.getCompanyName()) ||
                "Customer Fuel Requests".equalsIgnoreCase(customer.getCompanyName())
            )) {
            if (driverName != null && !driverName.trim().isEmpty()) {
                return driverName;
            }
        }
        return customer != null ? customer.getCompanyName() : null;
    }

    public String getProductName() {
        return product != null ? product.getProductName() : null;
    }
}
