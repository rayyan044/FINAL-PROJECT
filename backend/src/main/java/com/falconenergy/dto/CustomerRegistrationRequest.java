package com.falconenergy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegistrationRequest {
    @NotBlank @Size(max = 150) private String companyName;
    @NotBlank @Size(max = 100) private String contactPerson;
    @Email @NotBlank @Size(max = 100) private String companyEmail;
    @Size(max = 20) private String companyPhone;
    private String address;
    @Size(max = 50) private String tinNumber;
    @NotBlank @Size(min = 3, max = 50) private String username;
    @NotBlank @Size(max = 50) private String firstName;
    @NotBlank @Size(max = 50) private String lastName;
    @Email @NotBlank @Size(max = 100) private String email;
    @Size(max = 20) private String phone;
    @NotBlank @Size(min = 6, max = 100) private String password;
    @NotBlank private String confirmPassword;
}
