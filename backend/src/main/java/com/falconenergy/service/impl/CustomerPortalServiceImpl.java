package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.*;
import com.falconenergy.service.CustomerPortalService;
import com.falconenergy.service.FuelOrderService;
import com.falconenergy.util.DeliveryNotePdfGenerator;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerPortalServiceImpl implements CustomerPortalService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final FuelOrderService fuelOrderService;

    private Customer customer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication is required.");
        User user = userRepository.findByEmail(auth.getName()).or(() -> userRepository.findByUsername(auth.getName()))
                .orElseThrow(() -> new AccessDeniedException("Account not found."));
        if (user.getRole() != UserRole.CUSTOMER || user.getCustomer() == null) {
            throw new AccessDeniedException("A linked customer account is required.");
        }
        return user.getCustomer();
    }

    @Override public CustomerPortalResponse.Dashboard dashboard() {
        Customer c = customer();
        List<FuelOrder> all = fuelOrderRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId());
        List<CustomerPortalResponse.Delivery> deliveryList = deliveriesFor(c.getId());
        long active = all.stream().filter(o -> !Set.of("DELIVERED", "COMPLETED", "REJECTED", "CANCELLED").contains(normalize(o.getOrderStatus()))).count();
        return CustomerPortalResponse.Dashboard.builder()
                .totalOrders(all.size()).activeOrders(active)
                .awaitingPayment(all.stream().filter(o -> "PENDING_PAYMENT".equals(paymentStatus(o))).count())
                .inTransit(deliveryList.stream().filter(d -> "IN_TRANSIT".equals(d.getDeliveryStatus())).count())
                .delivered(deliveryList.stream().filter(d -> Set.of("DELIVERED", "COMPLETED").contains(d.getDeliveryStatus())).count())
                .recentOrders(all.stream().limit(5).map(this::orderView).toList())
                .activeDeliveries(deliveryList.stream().filter(d -> !Set.of("DELIVERED", "COMPLETED", "CANCELLED").contains(d.getDeliveryStatus())).limit(5).toList())
                .recentDocuments(documentsFor(c.getId(), null).stream().limit(6).toList()).build();
    }
    @Override public CustomerPortalResponse.Profile profile() { return profile(customer()); }
    @Override public CustomerPortalResponse.Profile updateProfile(CustomerProfileUpdateRequest r) {
        Customer c = customer(); c.setContactPerson(r.getContactPerson()); c.setEmail(r.getEmail().trim().toLowerCase()); c.setPhone(r.getPhone()); c.setAddress(r.getAddress());
        return profile(customerRepository.save(c));
    }
    private CustomerPortalResponse.Profile profile(Customer c) { return CustomerPortalResponse.Profile.builder().customerId(c.getId()).companyName(c.getCompanyName()).customerCode(c.getCustomerCode()).contactPerson(c.getContactPerson()).email(c.getEmail()).phone(c.getPhone()).address(c.getAddress()).build(); }

    @Override @Transactional(readOnly = true) public List<CustomerPortalResponse.Order> orders() { Customer c=customer(); return fuelOrderRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId()).stream().map(this::orderView).toList(); }
    @Override public CustomerPortalResponse.Order createOrder(CustomerPortalOrderRequest r) {
        Customer c = customer();
        FuelOrderRequest request = FuelOrderRequest.builder().orderNumber("ORD-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .customerId(c.getId()).productId(r.getProductId()).quantity(r.getQuantity()).deliveryDate(r.getDeliveryDate())
                .locationAddress(r.getDeliveryAddress()).locationLandmark(r.getLocationLandmark()).paymentMethod(r.getPaymentMethod()).notes(r.getNotes()).destination(r.getDestination()).build();
        return orderViewFromResponse(fuelOrderService.createOrder(request));
    }
    @Override @Transactional(readOnly = true) public CustomerPortalResponse.Order order(Long id) { return orderView(requireOrder(id)); }
    private FuelOrder requireOrder(Long id) { Customer c=customer(); return fuelOrderRepository.findById(id).filter(o -> o.getCustomer()!=null && c.getId().equals(o.getCustomer().getId())).orElseThrow(() -> new ResourceNotFoundException("Order not found.")); }
    private CustomerPortalResponse.Order orderView(FuelOrder o) {
        Invoice i=o.getInvoice();
        return CustomerPortalResponse.Order.builder().id(o.getId()).orderNumber(o.getOrderNumber()).productId(o.getProduct()==null?null:o.getProduct().getId()).productName(o.getProduct()==null?null:o.getProduct().getProductName()).quantity(o.getQuantity()).totalAmount(o.getAmount()).unitPrice(o.getUnitPrice()).orderDate(o.getOrderDate()).deliveryDate(o.getDeliveryDate()).status(o.getOrderStatus()).customerStatus(customerStatus(o)).paymentStatus(i==null?null:i.getPaymentStatus()).invoiceId(i==null?null:i.getId()).destination(o.getDestination()!=null?o.getDestination():o.getLocationAddress()).build();
    }
    private CustomerPortalResponse.Order orderViewFromResponse(FuelOrderResponse o) { return CustomerPortalResponse.Order.builder().id(o.getId()).orderNumber(o.getOrderNumber()).productId(o.getProduct()==null?null:o.getProduct().getId()).productName(o.getProductName()).quantity(o.getQuantity()).totalAmount(o.getAmount()).unitPrice(o.getUnitPrice()).orderDate(o.getOrderDate()).deliveryDate(o.getDeliveryDate()).status(o.getOrderStatus()).customerStatus(statusLabel(o.getOrderStatus(), o.getPaymentStatus())).paymentStatus(o.getPaymentStatus()).invoiceId(o.getInvoiceId()).destination(o.getDestination()!=null?o.getDestination():o.getLocationAddress()).build(); }
    private String paymentStatus(FuelOrder o) { return o.getInvoice()==null ? null : o.getInvoice().getPaymentStatus(); }

    @Override @Transactional(readOnly = true) public CustomerPortalResponse.Timeline timeline(Long id) {
        FuelOrder o=requireOrder(id);
        int rank=timelineRank(o);
        String current=timelineStatus(rank);
        List<String[]> steps=List.of(new String[]{"SUBMITTED","Order Submitted"},new String[]{"CONFIRMED","Sales Confirmed"},new String[]{"INVOICED","Invoice Generated"},new String[]{"PAID","Payment Confirmed"},new String[]{"LOADING","Preparing for Loading"},new String[]{"LOADED","Loaded"},new String[]{"READY","Ready for Dispatch"},new String[]{"DISPATCHED","Dispatched"},new String[]{"TRANSIT","In Transit"},new String[]{"DELIVERED","Delivered"},new String[]{"COMPLETED","Completed"});
        // `complete` means that this business milestone has been reached.  The
        // current milestone is therefore complete too (rather than an unchecked
        // dot), while `current` remains available to the UI for emphasis.
        List<CustomerPortalResponse.TimelineStep> response=new ArrayList<>(); for(int i=0;i<steps.size();i++) response.add(CustomerPortalResponse.TimelineStep.builder().key(steps.get(i)[0]).label(steps.get(i)[1]).complete(i<=rank).current(i==rank).build());
        return CustomerPortalResponse.Timeline.builder().currentStatus(current).steps(response).build();
    }
    /**
     * A driver's updates are recorded on Delivery, while the commercial order
     * status can remain at an earlier logistics state. Prefer the furthest
     * delivery state so customers see the actual truck progress.
     */
    private int timelineRank(FuelOrder order) {
        int orderRank = rank(order);
        int deliveryRank = deliveryRepository.findForCustomer(order.getCustomer().getId()).stream()
                .filter(delivery -> delivery.getLoadingOrder() != null
                        && delivery.getLoadingOrder().getOrder() != null
                        && Objects.equals(delivery.getLoadingOrder().getOrder().getId(), order.getId()))
                .mapToInt(delivery -> rank(delivery.getDeliveryStatus()))
                .max()
                .orElse(0);
        return Math.max(orderRank, deliveryRank);
    }
    private int rank(DeliveryStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case COMPLETED -> 10;
            case DELIVERED -> 9;
            case IN_TRANSIT, ARRIVED_AT_DESTINATION -> 8;
            case ASSIGNED, ACCEPTED -> 7;
            case CANCELLED -> 0;
        };
    }
    private String timelineStatus(int rank) {
        return switch (rank) {
            case 10 -> "Completed";
            case 9 -> "Delivered";
            case 8 -> "In Transit";
            case 7 -> "Dispatched";
            case 6 -> "Ready for Dispatch";
            case 5 -> "Loaded";
            case 4 -> "Preparing for Loading";
            case 3 -> "Payment Confirmed";
            case 2 -> "Confirmed";
            default -> "Under Review";
        };
    }
    private int rank(FuelOrder o) { String s=normalize(o.getOrderStatus()); if(Set.of("DELIVERED").contains(s)) return 9; if("COMPLETED".equals(s)) return 10; if("IN_TRANSIT".equals(s)) return 8; if("DISPATCHED".equals(s)) return 7; if(Set.of("READY_FOR_DISPATCH","DOCUMENTS_READY","DOCUMENTATION_PENDING").contains(s)) return 6; if(Set.of("LOADED","LOADING_IN_PROGRESS").contains(s)) return 5; if(Set.of("LOADING_ORDER_CREATED","LOADING_ORDER_APPROVED").contains(s)) return 4; if("PAYMENT_CONFIRMED".equals(s)) return 3; if("SALES_CONFIRMED".equals(s)) return 2; return 0; }
    private String customerStatus(FuelOrder o) { return statusLabel(o.getOrderStatus(), paymentStatus(o)); }
    private String statusLabel(String status,String payment) { String s=normalize(status); if("PENDING".equals(s)||"AWAITING_RESTOCK".equals(s)) return "Under Review"; if("SALES_CONFIRMED".equals(s)) return "Confirmed"; if("PENDING_PAYMENT".equalsIgnoreCase(payment)) return "Awaiting Payment"; if("PAYMENT_CONFIRMED".equals(s)||"PAID".equalsIgnoreCase(payment)) return "Payment Confirmed"; return switch(s){case "LOADING_ORDER_CREATED","LOADING_ORDER_APPROVED"->"Preparing for Loading";case "LOADING_IN_PROGRESS","LOADED"->"Loaded";case "DOCUMENTATION_PENDING","DOCUMENTS_READY","READY_FOR_DISPATCH"->"Ready for Dispatch";case "DISPATCHED"->"Dispatched";case "IN_TRANSIT"->"In Transit";case "DELIVERED"->"Delivered";case "COMPLETED"->"Completed";default->"Order Submitted";}; }
    private String normalize(String s) { return s==null?"":s.toUpperCase(Locale.ROOT); }

    @Override @Transactional(readOnly=true) public List<CustomerPortalResponse.Document> documents(Long orderId) { FuelOrder o=requireOrder(orderId); return documentsFor(customer().getId(), o.getId()); }
    private List<CustomerPortalResponse.Document> documentsFor(Long customerId, Long orderId) {
        List<CustomerPortalResponse.Document> out=new ArrayList<>();
        for(Invoice i: invoiceRepository.findByOrderCustomerIdOrderByInvoiceDateDesc(customerId)) if(orderId==null||i.getOrder().getId().equals(orderId)) out.add(CustomerPortalResponse.Document.builder().id(i.getId()).orderId(i.getOrder().getId()).type("Invoice").number(i.getInvoiceNumber()).status(i.getPaymentStatus()).endpoint("/customer-portal/invoices/"+i.getId()+"/pdf").availableAt(i.getInvoiceDate()).build());
        for(PaymentReceipt r: receiptRepository.findByInvoiceOrderCustomerIdOrderByCreatedAtDesc(customerId)) if(orderId==null||r.getInvoice().getOrder().getId().equals(orderId)) out.add(CustomerPortalResponse.Document.builder().id(r.getId()).orderId(r.getInvoice().getOrder().getId()).type("Payment Receipt").number(r.getReceiptNumber()).status(r.getReceiptStatus()).endpoint("/customer-portal/receipts/"+r.getId()+"/pdf").availableAt(r.getCreatedAt()).build());
        for(DeliveryNote n: deliveryNoteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)) { Long linked=n.getLoadingOrder()==null||n.getLoadingOrder().getOrder()==null?null:n.getLoadingOrder().getOrder().getId(); if(orderId==null||Objects.equals(orderId,linked)) out.add(CustomerPortalResponse.Document.builder().id(n.getId()).orderId(linked).type("Delivery Note").number(n.getDeliveryNoteNumber()).status(n.getStatus()).endpoint("/customer-portal/delivery-notes/"+n.getId()+"/pdf").availableAt(n.getCreatedAt()).build()); }
        return out.stream().sorted(Comparator.comparing(CustomerPortalResponse.Document::getAvailableAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }
    @Override @Transactional(readOnly=true) public List<CustomerPortalResponse.Invoice> invoices(){Customer c=customer();return invoiceRepository.findByOrderCustomerIdOrderByInvoiceDateDesc(c.getId()).stream().map(this::invoiceView).toList();}
    @Override @Transactional(readOnly=true) public CustomerPortalResponse.Invoice invoice(Long id){Customer c=customer();return invoiceView(invoiceRepository.findByIdAndOrderCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found.")));}
    private CustomerPortalResponse.Invoice invoiceView(Invoice i){return CustomerPortalResponse.Invoice.builder().id(i.getId()).invoiceNumber(i.getInvoiceNumber()).orderId(i.getOrder().getId()).orderNumber(i.getOrder().getOrderNumber()).productName(i.getOrder().getProduct().getProductName()).quantity(i.getOrder().getQuantity()).grandTotal(i.getGrandTotal()).paymentStatus(i.getPaymentStatus()).invoiceDate(i.getInvoiceDate()).invoiceType("Invoice").build();}
    @Override public byte[] invoicePdf(Long id){CustomerPortalResponse.Invoice i=invoice(id);return pdf("Invoice "+i.getInvoiceNumber(),List.of("Order: "+i.getOrderNumber(),"Fuel: "+i.getProductName(),"Quantity: "+i.getQuantity(),"Amount: "+i.getGrandTotal(),"Payment status: "+i.getPaymentStatus()));}
    @Override @Transactional(readOnly=true) public List<CustomerPortalResponse.Receipt> receipts(){Customer c=customer();return receiptRepository.findByInvoiceOrderCustomerIdOrderByCreatedAtDesc(c.getId()).stream().map(this::receiptView).toList();}
    @Override @Transactional(readOnly=true) public CustomerPortalResponse.Receipt receipt(Long id){Customer c=customer();return receiptView(receiptRepository.findByIdAndInvoiceOrderCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Payment receipt not found.")));}
    private CustomerPortalResponse.Receipt receiptView(PaymentReceipt r){return CustomerPortalResponse.Receipt.builder().id(r.getId()).invoiceId(r.getInvoice().getId()).invoiceNumber(r.getInvoice().getInvoiceNumber()).receiptNumber(r.getReceiptNumber()).status(r.getReceiptStatus()).amount(r.getReceivedAmount()).receivedAt(r.getReceivedAt()).build();}
    @Override public byte[] receiptPdf(Long id){CustomerPortalResponse.Receipt r=receipt(id);return pdf("Payment Receipt "+r.getReceiptNumber(),List.of("Invoice: "+r.getInvoiceNumber(),"Amount received: "+r.getAmount(),"Received on: "+r.getReceivedAt(),"Status: "+r.getStatus()));}
    private byte[] pdf(String title,List<String> lines){try{ByteArrayOutputStream out=new ByteArrayOutputStream();Document d=new Document();PdfWriter.getInstance(d,out);d.open();d.add(new Paragraph("FALCON ENERGY"));d.add(new Paragraph(title));d.add(new Paragraph(" "));for(String line:lines)d.add(new Paragraph(line));d.close();return out.toByteArray();}catch(Exception e){throw new IllegalStateException("Unable to generate document.",e);}}
    @Override @Transactional(readOnly=true) public List<CustomerPortalResponse.Delivery> deliveries(){return deliveriesFor(customer().getId());}
    private List<CustomerPortalResponse.Delivery> deliveriesFor(Long c){return deliveryRepository.findForCustomer(c).stream().map(this::deliveryView).toList();}
    @Override @Transactional(readOnly=true) public CustomerPortalResponse.Delivery delivery(Long id){Customer c=customer();return deliveryView(deliveryRepository.findForCustomerById(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Delivery not found.")));}
    private CustomerPortalResponse.Delivery deliveryView(Delivery d){FuelOrder o=d.getLoadingOrder()==null?null:d.getLoadingOrder().getOrder();return CustomerPortalResponse.Delivery.builder().id(d.getId()).deliveryNumber(d.getDeliveryNumber()).orderId(o==null?null:o.getId()).orderNumber(o==null?null:o.getOrderNumber()).deliveryNoteId(d.getDeliveryNote()==null?null:d.getDeliveryNote().getId()).truckNumber(d.getTruckNumber()).destination(d.getDestination()).dispatchStatus(d.getDispatch()==null?null:String.valueOf(d.getDispatch().getDispatchStatus())).deliveryStatus(String.valueOf(d.getDeliveryStatus())).dispatchedAt(d.getDispatchedAt()).deliveredAt(d.getDeliveredAt()).build();}
    @Override @Transactional(readOnly=true) public CustomerPortalResponse.Document deliveryNote(Long id){Customer c=customer();DeliveryNote n=deliveryNoteRepository.findByIdAndCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Delivery note not found."));Long orderId=n.getLoadingOrder()==null?null:n.getLoadingOrder().getOrder().getId();return CustomerPortalResponse.Document.builder().id(n.getId()).orderId(orderId).type("Delivery Note").number(n.getDeliveryNoteNumber()).status(n.getStatus()).endpoint("/customer-portal/delivery-notes/"+id+"/pdf").availableAt(n.getCreatedAt()).build();}
    @Override @Transactional(readOnly=true) public byte[] deliveryNotePdf(Long id){Customer c=customer();DeliveryNote n=deliveryNoteRepository.findByIdAndCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Delivery note not found."));if(n.getDelivery()==null)throw new ResourceNotFoundException("A printable delivery note is not available yet.");return DeliveryNotePdfGenerator.generate(n.getDelivery());}
}
