package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status; // ACTIVE, INACTIVE
}
