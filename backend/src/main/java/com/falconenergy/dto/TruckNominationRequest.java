package com.falconenergy.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckNominationRequest {
    private Long orderId;
    private String transportSource; // CUSTOMER_TRUCKS, FALCON_ARRANGED
    private Integer numberOfTrucks;
    private String confirmationNotes;
    private List<TruckNominationItemDto> items;
}
