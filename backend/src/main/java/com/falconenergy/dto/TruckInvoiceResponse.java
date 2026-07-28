package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long loadingOrderId;
    private Long loadingActivityId;
    private Long deliveryNoteId;
    private String deliveryNoteNumber;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productName;
    private String truckNumber;
    private String driverName;
    private String transportCompany;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String invoiceStatus;
    private LocalDateTime createdAt;
}
