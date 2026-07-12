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
    private Long paymentAccountId;
    private String paymentMethod;
    private String beneficiaryName;
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String swiftCode;
    private String paymentAccountCurrency;
    private String paymentTerms;
    private String paymentInstructions;
    private LocalDateTime validityDate;
    private String invoiceType;
    private CompanySettingsResponse companyDetails;
    private String fuelCategory;
    private String productSpecification;
    private String productDescription;
    private String unitOfMeasurement;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
