package com.falconenergy.mapper;

import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.entity.Delivery;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {
    
    public DeliveryResponse toResponse(Delivery d) {
        if (d == null) return null;
        return DeliveryResponse.builder()
                .id(d.getId())
                .deliveryNumber(d.getDeliveryNumber())
                .dispatchId(d.getDispatch() != null ? d.getDispatch().getId() : null)
                .dispatchNumber(d.getDispatch() != null ? d.getDispatch().getDispatchNumber() : null)
                .loadingOrderId(d.getLoadingOrder() != null ? d.getLoadingOrder().getId() : null)
                .loadingOrderNumber(d.getLoadingOrder() != null ? d.getLoadingOrder().getLoadingOrderNumber() : null)
                .invoiceId(d.getLoadingOrder() != null && d.getLoadingOrder().getOrder() != null && d.getLoadingOrder().getOrder().getInvoice() != null ? d.getLoadingOrder().getOrder().getInvoice().getId() : null)
                .invoiceNumber(d.getLoadingOrder() != null && d.getLoadingOrder().getOrder() != null && d.getLoadingOrder().getOrder().getInvoice() != null ? d.getLoadingOrder().getOrder().getInvoice().getInvoiceNumber() : null)
                .customerName(com.falconenergy.util.BuyerNameResolver.resolveName(d.getLoadingOrder()))
                .loadingActivityId(d.getLoadingActivity() != null ? d.getLoadingActivity().getId() : null)
                .deliveryNoteId(d.getDeliveryNote() != null ? d.getDeliveryNote().getId() : null)
                .deliveryNoteNumber(d.getDeliveryNote() != null ? d.getDeliveryNote().getDeliveryNoteNumber() : null)
                .truckInvoiceId(d.getTruckInvoice() != null ? d.getTruckInvoice().getId() : null)
                .truckInvoiceNumber(d.getTruckInvoice() != null ? d.getTruckInvoice().getInvoiceNumber() : null)
                .truckNumber(d.getTruckNumber())
                .driverName(d.getDriverName())
                .transportCompany(d.getTransportCompany())
                .destination(d.getDestination())
                .deliveryStatus(d.getDeliveryStatus() != null ? d.getDeliveryStatus().name() : null)
                .dispatchedAt(d.getDispatchedAt())
                .arrivalTime(d.getArrivalTime())
                .deliveredAt(d.getDeliveredAt())
                .receivedBy(d.getReceivedBy())
                .completedBy(d.getCompletedBy())
                .remarks(d.getRemarks())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
