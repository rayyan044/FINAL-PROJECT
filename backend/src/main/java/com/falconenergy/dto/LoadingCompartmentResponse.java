package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingCompartmentResponse {
    private Long id;
    private int compartmentNumber;
    private BigDecimal capacity;
    private Long productId;
    private String productNameSnapshot;
    private String productCodeSnapshot;
    private BigDecimal ambientVolume;
    private BigDecimal temperature;
    private BigDecimal density;
    private BigDecimal standardVolume;
    private String sealNumber;
}
