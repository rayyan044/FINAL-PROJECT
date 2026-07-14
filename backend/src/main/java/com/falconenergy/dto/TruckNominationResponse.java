package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckNominationResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String transportSource;
    private Integer numberOfTrucks;
    private String confirmationNotes;
    private String status;
    private BigDecimal totalAllocatedQuantity;
    private String rejectionReason;
    private List<TruckNominationItemDto> items;
}
