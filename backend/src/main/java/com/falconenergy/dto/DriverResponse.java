package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String licenseNumber;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
