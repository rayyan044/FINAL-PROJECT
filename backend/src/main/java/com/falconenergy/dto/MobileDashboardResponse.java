package com.falconenergy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dashboard data scoped exclusively to the driver represented by the JWT. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileDashboardResponse {
    private DriverInfo driver;
    private Summary summary;
    private List<RecentDelivery> recentDeliveries;
    private NotificationSummary notifications;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DriverInfo {
        private Long driverId;
        private String driverName;
        private VehicleInfo assignedVehicle;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VehicleInfo {
        private Long vehicleId;
        private String truckNumber;
        private String plateNumber;
        private String status;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Summary {
        private long assignedDeliveries;
        private long pendingDeliveries;
        private long deliveriesInProgress;
        private long completedToday;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentDelivery {
        private Long deliveryId;
        private String deliveryNoteNumber;
        private String customerName;
        private String fuelProduct;
        private BigDecimal quantity;
        private String destination;
        private String currentStatus;
        private LocalDate scheduledDeliveryDate;
        private Double startLatitude;
        private Double startLongitude;
        private Double podLatitude;
        private Double podLongitude;
        private String podPhotoPath;
        private String podNotes;
        private java.time.LocalDateTime podUploadedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NotificationSummary {
        private long unreadCount;
    }
}
