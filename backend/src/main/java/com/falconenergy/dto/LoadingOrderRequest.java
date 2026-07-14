package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingOrderRequest {
    private Long orderId;
    private LocalDate loadingDate;
    private String loadingTerminal;
    private String consignee;
    private String loadingRemarks;
    private String vesselName;
    private String operationsManager;
    private List<LoadingActivityRequest> activities;
}
