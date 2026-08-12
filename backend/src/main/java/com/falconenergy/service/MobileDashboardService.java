package com.falconenergy.service;

import com.falconenergy.dto.DriverProfileResponse;
import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.dto.NotificationResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MobileDashboardService {
    MobileDashboardResponse getDashboard();
    List<MobileDashboardResponse.RecentDelivery> getDeliveries();
    MobileDashboardResponse.RecentDelivery getDelivery(Long deliveryId);

    void acceptDelivery(Long deliveryId);
    void startTrip(Long deliveryId, Double latitude, Double longitude);
    void arriveAtDestination(Long deliveryId, String receivedBy, String remarks);
    void uploadProof(Long deliveryId, MultipartFile file, Double latitude, Double longitude, String notes);
    void completeDelivery(Long deliveryId);

    DriverProfileResponse getProfile();
    List<NotificationResponse> getNotifications();
    NotificationResponse getNotificationById(Long id);
    void markNotificationAsRead(Long notificationId);
 
    com.falconenergy.dto.DeliveryNoteResponse getDeliveryNoteForDelivery(Long deliveryId);
    byte[] getDeliveryNotePdf(Long deliveryId);
    String getDeliveryNoteNumber(Long deliveryId);
}
