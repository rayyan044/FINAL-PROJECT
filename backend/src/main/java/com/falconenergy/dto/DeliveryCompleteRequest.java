package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCompleteRequest {
    @NotBlank(message = "Completed by is required to complete delivery")
    private String completedBy;
    private String remarks;
}
