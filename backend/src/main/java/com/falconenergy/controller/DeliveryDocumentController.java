package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DeliveryNoteResponse;
import com.falconenergy.dto.TruckInvoiceResponse;
import com.falconenergy.service.DeliveryDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
public class DeliveryDocumentController {

    private final DeliveryDocumentService deliveryDocumentService;

    @PostMapping("/delivery-notes/loading-activity/{activityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> generateDeliveryNote(@PathVariable Long activityId) {
        DeliveryNoteResponse response = deliveryDocumentService.generateDeliveryNote(activityId);
        return ResponseEntity.ok(ApiResponse.success("Delivery Note generated successfully", response));
    }

    @PostMapping("/truck-invoices/loading-activity/{activityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckInvoiceResponse>> generateTruckInvoice(@PathVariable Long activityId) {
        TruckInvoiceResponse response = deliveryDocumentService.generateTruckInvoice(activityId);
        return ResponseEntity.ok(ApiResponse.success("Truck Invoice generated successfully", response));
    }

    @GetMapping("/delivery-notes/loading-activity/{activityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> getDeliveryNoteByActivity(@PathVariable Long activityId) {
        try {
            DeliveryNoteResponse response = deliveryDocumentService.getDeliveryNoteByActivity(activityId);
            return ResponseEntity.ok(ApiResponse.success("Delivery Note retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Delivery Note not found", null));
        }
    }

    @GetMapping("/truck-invoices/loading-activity/{activityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckInvoiceResponse>> getTruckInvoiceByActivity(@PathVariable Long activityId) {
        try {
            TruckInvoiceResponse response = deliveryDocumentService.getTruckInvoiceByActivity(activityId);
            return ResponseEntity.ok(ApiResponse.success("Truck Invoice retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Truck Invoice not found", null));
        }
    }

    @PostMapping("/delivery-notes/{id}/print")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> printDeliveryNote(@PathVariable Long id) {
        DeliveryNoteResponse response = deliveryDocumentService.printDeliveryNote(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery Note printed successfully", response));
    }

    @PostMapping("/truck-invoices/{id}/print")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckInvoiceResponse>> printTruckInvoice(@PathVariable Long id) {
        TruckInvoiceResponse response = deliveryDocumentService.printTruckInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Truck Invoice printed successfully", response));
    }

    @PostMapping("/delivery-notes/{id}/handover")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> markHandedToDriver(@PathVariable Long id) {
        DeliveryNoteResponse response = deliveryDocumentService.markHandedToDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery Note marked as handed to driver", response));
    }
}
