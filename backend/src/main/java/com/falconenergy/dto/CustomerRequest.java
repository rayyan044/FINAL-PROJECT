package com.falconenergy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @NotBlank(message = "Customer code is required")
    @Size(max = 50, message = "Customer code cannot exceed 50 characters")
    private String customerCode;

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name cannot exceed 150 characters")
    private String companyName;

    @NotBlank(message = "Contact person is required")
    @Size(max = 100, message = "Contact person name cannot exceed 100 characters")
    private String contactPerson;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    private String address;

    @Size(max = 50, message = "TIN number cannot exceed 50 characters")
    private String tinNumber;

    @Builder.Default
    private String status = "ACTIVE";
}
