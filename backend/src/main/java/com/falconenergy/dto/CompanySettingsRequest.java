package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettingsRequest {
    private String companyName;
    private String postalAddress;
    private String officeAddress;
    private String phoneNumber;
    private String email;
    private String logo;
    private String signatoryName;
    private String signatoryTitle;
    private String signatorySignature;
    private String stamp;
}
