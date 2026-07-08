package com.falconenergy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRequest {

    @NotBlank(message = "Delivery number is required")
    private String deliveryNumber;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    @Builder.Default
    private String deliveryStatus = "PENDING"; // PENDING, EN_ROUTE, ARRIVED, DELIVERED, CANCELLED
}
