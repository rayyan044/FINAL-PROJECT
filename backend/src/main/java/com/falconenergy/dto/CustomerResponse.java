package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String companyName;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String tinNumber;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
