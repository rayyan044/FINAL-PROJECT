package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    private String licenseNumber;

    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, BUSY, INACTIVE
}
