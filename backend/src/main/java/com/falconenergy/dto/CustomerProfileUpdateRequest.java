package com.falconenergy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerProfileUpdateRequest {
    @NotBlank @Size(max = 100) private String contactPerson;
    @Email @NotBlank @Size(max = 100) private String email;
    @Size(max = 20) private String phone;
    private String address;
}
