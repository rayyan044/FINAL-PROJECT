package com.falconenergy.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Deliberately small, customer-safe views; internal records are never returned directly. */
public final class CustomerPortalResponse {
    private CustomerPortalResponse() { }

    @Getter @Builder public static class Dashboard {
        private long totalOrders, activeOrders, awaitingPayment, inTransit, delivered;
        private List<Order> recentOrders;
        private List<Delivery> activeDeliveries;
        private List<Document> recentDocuments;
    }
    @Getter @Builder public static class Profile {
        private Long customerId;
        private String companyName, customerCode, contactPerson, email, phone, address;
    }
    @Getter @Builder public static class Order {
        private Long id, productId, invoiceId;
        private String orderNumber, productName, status, customerStatus, paymentStatus, destination;
        private BigDecimal quantity, totalAmount, unitPrice;
        private LocalDateTime orderDate, deliveryDate;
    }
    @Getter @Builder public static class Timeline {
        private String currentStatus;
        private List<TimelineStep> steps;
    }
    @Getter @Builder public static class TimelineStep {
        private String key, label;
        private boolean complete, current;
    }
    @Getter @Builder public static class Invoice {
        private Long id, orderId;
        private String invoiceNumber, orderNumber, productName, paymentStatus, invoiceType;
        private BigDecimal quantity, grandTotal;
        private LocalDateTime invoiceDate;
    }
    @Getter @Builder public static class Receipt {
        private Long id, invoiceId;
        private String receiptNumber, invoiceNumber, status;
        private BigDecimal amount;
        private LocalDateTime receivedAt;
    }
    @Getter @Builder public static class Delivery {
        private Long id, orderId, deliveryNoteId;
        private String deliveryNumber, orderNumber, truckNumber, destination, dispatchStatus, deliveryStatus;
        private LocalDateTime dispatchedAt, deliveredAt;
    }
    @Getter @Builder public static class DeliveryTracking {
        private Long deliveryId;
        private String deliveryNumber, truckNumber, status;
        private boolean live;
        private Double latitude, longitude, accuracy;
        private LocalDateTime updatedAt;
    }
    @Getter @Builder public static class Document {
        private Long id, orderId;
        private String type, number, status, endpoint;
        private LocalDateTime availableAt;
    }
}
