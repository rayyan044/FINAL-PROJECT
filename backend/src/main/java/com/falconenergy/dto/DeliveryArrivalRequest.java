package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryArrivalRequest {
    private String receivedBy;
    private String remarks;
}
