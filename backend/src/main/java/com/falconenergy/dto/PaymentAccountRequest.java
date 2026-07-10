package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAccountRequest {
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
}
