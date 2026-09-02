package com.falconenergy.service.impl;
import com.falconenergy.dto.PaymentReceiptResponse; import com.falconenergy.entity.*; import com.falconenergy.exception.ResourceNotFoundException; import com.falconenergy.repository.*; import com.falconenergy.service.PaymentReceiptService; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*; import java.time.format.DateTimeFormatter; import java.math.BigDecimal;
@Service @RequiredArgsConstructor @Transactional
public class PaymentReceiptServiceImpl implements PaymentReceiptService {
 private final PaymentReceiptRepository repository;
 private final InvoiceRepository invoiceRepository; private final LoadingOrderRepository loadingOrderRepository; private final LoadingReportRepository loadingReportRepository; private final DeliveryNoteRepository deliveryNoteRepository; private final CompanySettingsRepository companySettingsRepository;
 private final PaymentRepository paymentRepository;
 public PaymentReceiptResponse generateForPaidInvoice(Invoice invoice) { Invoice detailed = invoiceRepository.findDetailedById(invoice.getId()).orElseThrow(() -> new ResourceNotFoundException("Customer invoice not found with id: " + invoice.getId())); if (!"PAID".equalsIgnoreCase(detailed.getPaymentStatus())) throw new IllegalArgumentException("A payment receipt can only be issued for a paid customer invoice."); return repository.findByInvoiceId(detailed.getId()).map(this::map).orElseGet(() -> map(repository.save(PaymentReceipt.builder().receiptNumber(nextNumber()).invoice(detailed).receiptStatus("PAID").receivedAmount(detailed.getGrandTotal()).receivedAt(detailed.getFinanceApprovedAt() != null ? detailed.getFinanceApprovedAt() : LocalDateTime.now()).confirmedBy(detailed.getFinanceApprovedBy()).build()))); }
 @Transactional(readOnly=true) public PaymentReceiptResponse getByInvoiceId(Long id) { return map(repository.findByInvoiceId(id).orElseThrow(() -> new ResourceNotFoundException("Payment Receipt not found for customer invoice id: " + id))); }
 @Transactional(readOnly=true) public boolean existsForInvoiceId(Long id) { return repository.existsByInvoiceId(id); }
 private synchronized String nextNumber() { String prefix="PR-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"; String max=repository.findMaxReceiptNumberWithPrefix(prefix); int n=max==null?1:Integer.parseInt(max.substring(prefix.length()))+1; return prefix+String.format("%04d",n); }
    private PaymentReceiptResponse map(PaymentReceipt r) {
        Invoice i = invoiceRepository.findDetailedById(r.getInvoice().getId()).orElseThrow();
        FuelOrder order = i.getOrder();
        Customer customer = com.falconenergy.util.BuyerNameResolver.resolveCustomer(order);
        CompanySettings company = companySettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        LoadingOrder loadingOrder = loadingOrderRepository.findByOrderId(order.getId()).orElse(null);
        LoadingActivity activity = loadingOrder != null && loadingOrder.getActivities() != null && !loadingOrder.getActivities().isEmpty() ? loadingOrder.getActivities().get(0) : null;
        LoadingReport report = activity == null ? null : loadingReportRepository.findFirstByLoadingActivityIdOrderByCreatedAtDesc(activity.getId()).orElse(null);
        DeliveryNote note = activity == null ? null : deliveryNoteRepository.findByLoadingActivityId(activity.getId()).orElse(null);
        String paymentMethod = i.getPaymentMethod() != null && !i.getPaymentMethod().isBlank() ? i.getPaymentMethod() : order.getPaymentMethod();
        Payment gatewayPayment = paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(i.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED).findFirst().orElse(null);

        boolean isEmergency = customer != null && (
                "EMERGENCY".equalsIgnoreCase(customer.getCustomerCode()) ||
                "Stranded Drivers (Emergency Requests)".equalsIgnoreCase(customer.getCompanyName()) ||
                "Customer Fuel Requests".equalsIgnoreCase(customer.getCompanyName())
        );

        String resolvedCustomerName = isEmergency && order.getDriverName() != null && !order.getDriverName().trim().isEmpty()
                ? order.getDriverName() : (customer != null ? customer.getCompanyName() : null);

        String finalContactPerson = isEmergency ? "Emergency Operator" : (customer != null ? customer.getContactPerson() : null);

        String finalContactPhone = isEmergency && order.getDriverPhone() != null && !order.getDriverPhone().trim().isEmpty()
                ? order.getDriverPhone() : (customer != null && customer.getPhone() != null ? customer.getPhone() : "Customer contact not captured");

        String finalContactEmail = isEmergency && order.getDriverEmail() != null && !order.getDriverEmail().trim().isEmpty()
                ? order.getDriverEmail() : (customer != null && customer.getEmail() != null ? customer.getEmail() : "Customer email not captured");

        return PaymentReceiptResponse.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .companyName(company != null ? company.getCompanyName() : "FALCON ENERGY LTD.")
                .companyAddress(company != null ? company.getOfficeAddress() : "Company address not configured")
                .companyPhone(company != null ? company.getPhoneNumber() : "Company contact not configured")
                .companyEmail(company != null ? company.getEmail() : "Company email not configured")
                .companyLogo(company != null ? company.getLogo() : null)
                .invoiceId(i.getId())
                .invoiceNumber(i.getInvoiceNumber())
                .invoiceDate(i.getInvoiceDate())
                .customerOrderNumber(order.getOrderNumber())
                .customerName(resolvedCustomerName)
                .customerCompany(resolvedCustomerName)
                .customerContactPerson(finalContactPerson)
                .customerContact(finalContactPhone)
                .customerEmail(finalContactEmail)
                .productName(order.getProduct().getProductName())
                .fuelQuantity(order.getQuantity())
                .currency(order.getCurrency())
                .paymentMethod(gatewayPayment != null ? gatewayPayment.getPaymentMethod() : (paymentMethod != null ? paymentMethod : "Payment method not captured"))
                .paymentReference(gatewayPayment != null ? gatewayPayment.getPaymentReference() : "Not captured")
                .bankName(i.getBankName() != null ? i.getBankName() : "Not applicable for local simulation")
                // The invoice subtotal includes fuel and the single transport charge. Keep the
                // receipt breakdown truthful by showing fuel supply independently.
                .fuelSupplyAmount(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount())
                .transportChargeAmount(i.getTransportCharges() == null ? BigDecimal.ZERO : i.getTransportCharges())
                .receivedAmount(r.getReceivedAmount())
                .receivedAt(r.getReceivedAt())
                .paymentConfirmedAt(i.getFinanceApprovedAt())
                .financeOfficerName(i.getFinanceApprovedBy() != null ? i.getFinanceApprovedBy() : "Finance officer not captured")
                .receivedBy(i.getFinanceApprovedBy() != null ? i.getFinanceApprovedBy() : "Finance officer not captured")
                .confirmedBy(r.getConfirmedBy())
                .receiptStatus("PAID")
                .loadingOrderNumber(loadingOrder == null ? "Loading order not yet created" : loadingOrder.getLoadingOrderNumber())
                .loadingReportNumber(report == null ? "Loading report not yet available" : report.getReportNumber())
                .deliveryNoteNumber(note == null ? "Delivery Note not yet generated" : note.getDeliveryNoteNumber())
                .generatedAt(r.getCreatedAt())
                .build();
    }
}
