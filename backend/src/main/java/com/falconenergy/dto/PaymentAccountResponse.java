package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAccountResponse {
    private Long id;
    private String paymentMethod;
    private String beneficiaryName;
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String swiftCode;
    private String currency;
    private String paymentTerms;
    private String paymentInstructions;
    private String status;
    private Integer validityDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
