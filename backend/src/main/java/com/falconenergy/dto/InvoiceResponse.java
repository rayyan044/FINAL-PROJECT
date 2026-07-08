package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private FuelOrderResponse order;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal grandTotal;
    private String paymentStatus;
    private String financeApprovedBy;
    private LocalDateTime financeApprovedAt;
    private String termsAndConditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
