package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingCompartmentRequest {
    private int compartmentNumber;
    private BigDecimal capacity;
    private Long productId;
    private BigDecimal ambientVolume;
    private BigDecimal temperature;
    private BigDecimal density;
    private String sealNumber;
}
