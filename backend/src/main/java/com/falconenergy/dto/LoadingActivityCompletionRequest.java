package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingActivityCompletionRequest {
    private String bayNumber;
    private String pumpNumber;
    private BigDecimal meterStart;
    private BigDecimal meterEnd;
    private String remarks;
    private List<LoadingCompartmentRequest> compartments;
}
