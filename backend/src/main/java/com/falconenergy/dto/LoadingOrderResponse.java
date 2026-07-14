package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingOrderResponse {
    private Long id;
    private String loadingOrderNumber;
    private Long orderId;
    private String customerOrderNumber;
    private String customerName;
    private LocalDate loadingDate;
    private String product;
    private BigDecimal approvedQuantity;
    private String loadingTerminal;
    private String consignee;
    private Integer numberOfTrucks;
    private String preparedBy;
    private String approvedBy;
    private String status;
    private String loadingRemarks;
    private String vesselName;
    private String operationsManager;
    private List<LoadingActivityResponse> activities;
}
