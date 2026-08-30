package com.falconenergy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRegistrationRequest {
    @NotBlank @Size(max = 150) private String companyName;
    @NotBlank @Size(max = 100) private String contactPerson;
    @Email @NotBlank @Size(max = 100) private String companyEmail;
    @Size(max = 20) @Pattern(regexp = "^[67]\\d{8}$", message = "Company phone must be a 9-digit Tanzanian mobile number starting with 6 or 7") private String companyPhone;
    private String address;
    @Size(max = 50) private String tinNumber;
    @NotBlank @Size(min = 3, max = 50) private String username;
    @NotBlank @Size(max = 50) private String firstName;
    @NotBlank @Size(max = 50) private String lastName;
    @Email @NotBlank @Size(max = 100) private String email;
    @Size(max = 20) @Pattern(regexp = "^[67]\\d{8}$", message = "Phone must be a 9-digit Tanzanian mobile number starting with 6 or 7") private String phone;
    @NotBlank @Size(min = 6, max = 100) private String password;
    @NotBlank private String confirmPassword;
}
