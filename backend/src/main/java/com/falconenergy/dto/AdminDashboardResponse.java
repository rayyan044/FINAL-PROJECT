package com.falconenergy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long customers;
    private long fuelRequests;
    private long deliveries;
    private long salesOfficers;
}
