package com.falconenergy.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfProfileUpdateRequest {

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 100, message = "Confirm password cannot exceed 100 characters")
    private String confirmPassword;
}
