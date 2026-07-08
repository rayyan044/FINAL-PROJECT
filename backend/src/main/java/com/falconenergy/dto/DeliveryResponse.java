package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {
    private Long id;
    private String deliveryNumber;
    private DriverResponse driver;
    private VehicleResponse vehicle;
    private FuelOrderResponse order;
    private String deliveryStatus;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCustomerName() {
        return order != null && order.getCustomer() != null ? order.getCustomer().getCompanyName() : null;
    }

    public String getOrderNumber() {
        return order != null ? order.getOrderNumber() : null;
    }

    public String getDriverName() {
        return driver != null ? driver.getFirstName() + " " + driver.getLastName() : null;
    }

    public String getVehiclePlateNumber() {
        return vehicle != null ? vehicle.getPlateNumber() : null;
    }

    public java.math.BigDecimal getQuantity() {
        return order != null ? order.getQuantity() : null;
    }
}
