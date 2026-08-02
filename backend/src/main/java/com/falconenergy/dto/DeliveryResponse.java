package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {
    private Long id;
    private String deliveryNumber;
    private Long dispatchId;
    private String dispatchNumber;
    
    private Long loadingOrderId;
    private String loadingOrderNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private String customerName;
    private Long loadingActivityId;
    private Long deliveryNoteId;
    private String deliveryNoteNumber;
    private Long truckInvoiceId;
    private String truckInvoiceNumber;
    
    private String truckNumber;
    private String driverName;
    private String transportCompany;
    private String destination;
    private String deliveryStatus;
    
    private LocalDateTime dispatchedAt;
    private LocalDateTime arrivalTime;
    private LocalDateTime deliveredAt;
    
    private String receivedBy;
    private String completedBy;
    private String remarks;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper methods for backward compatibility
    public String getOrderNumber() {
        return loadingOrderNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getVehiclePlateNumber() {
        return truckNumber;
    }

    public BigDecimal getQuantity() {
        return null;
    }
}
