package com.falconenergy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAccountResponse {
    private Long userId;
    private Long driverId;
    private String driverName;
    private String username;
    private String accountStatus;
    private boolean enabled;
}
