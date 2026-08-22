package com.falconenergy.controller;

import com.falconenergy.dto.*;
import com.falconenergy.service.CustomerPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/customer-portal", "/api/customer-portal"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {
    private final CustomerPortalService service;

    @GetMapping("/dashboard") public ResponseEntity<ApiResponse<CustomerPortalResponse.Dashboard>> dashboard(){return ok("Customer dashboard retrieved",service.dashboard());}
    @GetMapping("/profile") public ResponseEntity<ApiResponse<CustomerPortalResponse.Profile>> profile(){return ok("Customer profile retrieved",service.profile());}
    @PutMapping("/profile") public ResponseEntity<ApiResponse<CustomerPortalResponse.Profile>> profile(@Valid @RequestBody CustomerProfileUpdateRequest r){return ok("Customer profile updated",service.updateProfile(r));}
    @GetMapping("/orders") public ResponseEntity<ApiResponse<List<CustomerPortalResponse.Order>>> orders(){return ok("Customer orders retrieved",service.orders());}
    @PostMapping("/orders") public ResponseEntity<ApiResponse<CustomerPortalResponse.Order>> order(@Valid @RequestBody CustomerPortalOrderRequest r){return ok("Fuel order submitted",service.createOrder(r));}
    @GetMapping("/orders/{id}") public ResponseEntity<ApiResponse<CustomerPortalResponse.Order>> order(@PathVariable Long id){return ok("Customer order retrieved",service.order(id));}
    @GetMapping("/orders/{id}/timeline") public ResponseEntity<ApiResponse<CustomerPortalResponse.Timeline>> timeline(@PathVariable Long id){return ok("Order timeline retrieved",service.timeline(id));}
    @GetMapping("/orders/{id}/documents") public ResponseEntity<ApiResponse<List<CustomerPortalResponse.Document>>> docs(@PathVariable Long id){return ok("Order documents retrieved",service.documents(id));}
    @GetMapping("/invoices") public ResponseEntity<ApiResponse<List<CustomerPortalResponse.Invoice>>> invoices(){return ok("Customer invoices retrieved",service.invoices());}
    @GetMapping("/invoices/{id}") public ResponseEntity<ApiResponse<CustomerPortalResponse.Invoice>> invoice(@PathVariable Long id){return ok("Invoice retrieved",service.invoice(id));}
    @GetMapping("/invoices/{id}/pdf") public ResponseEntity<byte[]> invoicePdf(@PathVariable Long id){return pdf(service.invoicePdf(id),"invoice-"+id+".pdf");}
    @GetMapping("/receipts") public ResponseEntity<ApiResponse<List<CustomerPortalResponse.Receipt>>> receipts(){return ok("Payment receipts retrieved",service.receipts());}
    @GetMapping("/receipts/{id}") public ResponseEntity<ApiResponse<CustomerPortalResponse.Receipt>> receipt(@PathVariable Long id){return ok("Payment receipt retrieved",service.receipt(id));}
    @GetMapping("/receipts/{id}/pdf") public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id){return pdf(service.receiptPdf(id),"receipt-"+id+".pdf");}
    @GetMapping("/deliveries") public ResponseEntity<ApiResponse<List<CustomerPortalResponse.Delivery>>> deliveries(){return ok("Customer deliveries retrieved",service.deliveries());}
    @GetMapping("/deliveries/{id}") public ResponseEntity<ApiResponse<CustomerPortalResponse.Delivery>> delivery(@PathVariable Long id){return ok("Delivery retrieved",service.delivery(id));}
    @GetMapping("/delivery-notes/{id}") public ResponseEntity<ApiResponse<CustomerPortalResponse.Document>> deliveryNote(@PathVariable Long id){return ok("Delivery note retrieved",service.deliveryNote(id));}
    @GetMapping("/delivery-notes/{id}/pdf") public ResponseEntity<byte[]> deliveryNotePdf(@PathVariable Long id){return pdf(service.deliveryNotePdf(id),"delivery-note-"+id+".pdf");}
    private <T> ResponseEntity<ApiResponse<T>> ok(String message,T value){return ResponseEntity.ok(ApiResponse.success(message,value));}
    private ResponseEntity<byte[]> pdf(byte[] bytes,String name){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").body(bytes);}
}
