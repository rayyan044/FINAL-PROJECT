package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private String email;
    private String username;
    private String role;
    private boolean passwordChanged;
    private String phone;
    private String firstName;
    private String lastName;
    private Long driverId;
}
