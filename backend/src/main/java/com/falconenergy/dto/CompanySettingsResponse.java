package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettingsResponse {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
