package com.falconenergy.service;

import com.falconenergy.dto.DeliveryNoteResponse;
import com.falconenergy.dto.TruckInvoiceResponse;
import java.util.List;

public interface DeliveryDocumentService {
    DeliveryNoteResponse generateDeliveryNote(Long activityId);
    TruckInvoiceResponse generateTruckInvoice(Long activityId);
    DeliveryNoteResponse getDeliveryNoteByActivity(Long activityId);
    TruckInvoiceResponse getTruckInvoiceByActivity(Long activityId);
    DeliveryNoteResponse printDeliveryNote(Long noteId);
    TruckInvoiceResponse printTruckInvoice(Long invoiceId);
    DeliveryNoteResponse markHandedToDriver(Long noteId);
    List<DeliveryNoteResponse> getPendingDocumentationNotes();
}
