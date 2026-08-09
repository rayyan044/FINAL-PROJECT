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
public class DriverProfileResponse {
    private Long driverId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String licenseNumber;
    private String driverStatus;
    private MobileDashboardResponse.VehicleInfo assignedVehicle;
}
