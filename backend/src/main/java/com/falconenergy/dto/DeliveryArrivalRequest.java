package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryArrivalRequest {
    @NotBlank(message = "Received by is required to record arrival")
    private String receivedBy;
    private String remarks;
}
