package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private String status;
    private boolean passwordChanged;
    private Long driverId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
