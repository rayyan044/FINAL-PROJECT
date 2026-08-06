package com.falconenergy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The temporary password is returned once to an authorized administrator so it
 * can be communicated to the driver through an approved channel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverPasswordResetResponse {
    private Long driverId;
    private String username;
    private String temporaryPassword;
}
