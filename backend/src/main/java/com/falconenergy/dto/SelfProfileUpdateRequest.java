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

    private String password;

    private String confirmPassword;
}
