package com.falconenergy.dto;

import java.math.BigDecimal;
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
    private String depotName;
    private String depotAddress;
    private BigDecimal depotLatitude;
    private BigDecimal depotLongitude;
}
