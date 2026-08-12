package com.falconenergy.service;

import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.dto.DeliveryArrivalRequest;
import com.falconenergy.dto.DeliveryCompleteRequest;
import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.Driver;
import java.util.List;

public interface DeliveryService {
    List<DeliveryResponse> getActiveDeliveries();
    DeliveryResponse createDelivery(Long dispatchId);
    DeliveryResponse getDeliveryById(Long id);
    DeliveryResponse markArrived(Long deliveryId, DeliveryArrivalRequest request);
    DeliveryResponse completeDelivery(Long deliveryId, DeliveryCompleteRequest request);
    DeliveryResponse cancelDelivery(Long deliveryId, String remarks);
    List<DeliveryResponse> getDeliveryHistory();
    void sendDeliveryAssignedNotification(Delivery delivery, Driver driver);
}
