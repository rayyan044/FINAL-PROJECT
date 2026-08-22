package com.falconenergy.service;

import com.falconenergy.dto.*;
import java.util.List;

public interface CustomerPortalService {
    CustomerPortalResponse.Dashboard dashboard();
    CustomerPortalResponse.Profile profile();
    CustomerPortalResponse.Profile updateProfile(CustomerProfileUpdateRequest request);
    List<CustomerPortalResponse.Order> orders();
    CustomerPortalResponse.Order createOrder(CustomerPortalOrderRequest request);
    CustomerPortalResponse.Order order(Long id);
    CustomerPortalResponse.Timeline timeline(Long id);
    List<CustomerPortalResponse.Document> documents(Long orderId);
    List<CustomerPortalResponse.Invoice> invoices();
    CustomerPortalResponse.Invoice invoice(Long id);
    byte[] invoicePdf(Long id);
    List<CustomerPortalResponse.Receipt> receipts();
    CustomerPortalResponse.Receipt receipt(Long id);
    byte[] receiptPdf(Long id);
    List<CustomerPortalResponse.Delivery> deliveries();
    CustomerPortalResponse.Delivery delivery(Long id);
    CustomerPortalResponse.Document deliveryNote(Long id);
    byte[] deliveryNotePdf(Long id);
}
