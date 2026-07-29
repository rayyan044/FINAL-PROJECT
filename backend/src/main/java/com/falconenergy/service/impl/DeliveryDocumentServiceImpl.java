package com.falconenergy.service.impl;

import com.falconenergy.dto.DeliveryNoteResponse;
import com.falconenergy.dto.TruckInvoiceResponse;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.*;
import com.falconenergy.service.DeliveryDocumentService;
import com.falconenergy.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryDocumentServiceImpl implements DeliveryDocumentService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final TruckInvoiceRepository truckInvoiceRepository;
    private final LoadingActivityRepository loadingActivityRepository;
    private final LoadingReportRepository loadingReportRepository;
    private final LoadingOrderRepository loadingOrderRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final UserRepository userRepository;
    private final SystemSettingService systemSettingService;

    @Override
    public DeliveryNoteResponse generateDeliveryNote(Long activityId) {
        log.info("Generating delivery note for loading activity: {}", activityId);
        
        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading activity not found with id: " + activityId));

        if (activity.getStatus() != LoadingActivityStatus.COMPLETED) {
            throw new BadRequestException("Loading activity must be COMPLETED before generating a Delivery Note.");
        }

        LoadingReport report = loadingReportRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("Loading Report must exist before generating a Delivery Note."));

        if (deliveryNoteRepository.existsByLoadingActivityId(activityId)) {
            throw new BadRequestException("A Delivery Note has already been generated for this loading activity.");
        }

        LoadingOrder loadingOrder = activity.getLoadingOrder();
        FuelOrder fuelOrder = loadingOrder != null ? loadingOrder.getOrder() : null;
        Customer customer = fuelOrder != null ? fuelOrder.getCustomer() : null;
        FuelProduct product = fuelOrder != null ? fuelOrder.getProduct() : null;

        String username = resolveCurrentUser();
        String docNumber = generateDeliveryNoteNumber();

        DeliveryNote deliveryNote = DeliveryNote.builder()
                .deliveryNoteNumber(docNumber)
                .loadingOrder(loadingOrder)
                .loadingActivity(activity)
                .loadingReport(report)
                .customer(customer)
                .product(product)
                .truckNumber(activity.getTruckNumber())
                .driverName(activity.getDriverName())
                .driverLicenseNumber(activity.getDriverLicenceNumber())
                .transportCompany(activity.getTransportCompany())
                .truckCapacity(activity.getVehicle() != null ? activity.getVehicle().getCapacity() : null)
                .transportCharge(activity.getTransportCharge())
                .ambientVolume(activity.getAmbientVolume())
                .standardVolume(activity.getStandardVolume())
                .destination(activity.getDestination())
                .status("PREPARED")
                .preparedBy(username)
                .preparedAt(LocalDateTime.now())
                .build();

        deliveryNote.setCreatedBy(username);
        deliveryNote.setUpdatedBy(username);

        DeliveryNote saved = deliveryNoteRepository.save(deliveryNote);
        
        // Check if documents are ready for the order
        checkAndUpdateToDocumentsReady(loadingOrder);

        return toDeliveryNoteResponse(saved);
    }

    @Override
    public TruckInvoiceResponse generateTruckInvoice(Long activityId) {
        log.info("Generating truck invoice for loading activity: {}", activityId);

        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading activity not found with id: " + activityId));

        if (activity.getStatus() != LoadingActivityStatus.COMPLETED) {
            throw new BadRequestException("Loading activity must be COMPLETED before generating a Truck Invoice.");
        }

        DeliveryNote deliveryNote = deliveryNoteRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new BadRequestException("A Delivery Note must be generated first."));

        if (truckInvoiceRepository.existsByLoadingActivityId(activityId)) {
            throw new BadRequestException("A Truck Invoice has already been generated for this loading activity.");
        }

        LoadingOrder loadingOrder = activity.getLoadingOrder();
        FuelOrder fuelOrder = loadingOrder != null ? loadingOrder.getOrder() : null;
        Customer customer = fuelOrder != null ? fuelOrder.getCustomer() : null;
        FuelProduct product = fuelOrder != null ? fuelOrder.getProduct() : null;

        BigDecimal quantity = activity.getStandardVolume();
        if (quantity == null) {
            throw new BadRequestException("Loading Report Standard Volume is required for invoice calculation.");
        }

        BigDecimal unitPrice = product != null ? product.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = quantity.multiply(unitPrice);

        String username = resolveCurrentUser();
        String invNumber = generateInvoiceNumber();

        TruckInvoice invoice = TruckInvoice.builder()
                .invoiceNumber(invNumber)
                .loadingOrder(loadingOrder)
                .loadingActivity(activity)
                .deliveryNote(deliveryNote)
                .customer(customer)
                .product(product)
                .truckNumber(activity.getTruckNumber())
                .driverName(activity.getDriverName())
                .transportCompany(activity.getTransportCompany())
                .truckCapacity(activity.getVehicle() != null ? activity.getVehicle().getCapacity() : null)
                .transportCharge(activity.getTransportCharge())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .paymentStatus("PENDING_PAYMENT")
                .invoiceStatus("GENERATED")
                .build();

        invoice.setCreatedBy(username);
        invoice.setUpdatedBy(username);

        TruckInvoice saved = truckInvoiceRepository.save(invoice);

        // Check if documents are ready for the order
        checkAndUpdateToDocumentsReady(loadingOrder);

        return toTruckInvoiceResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryNoteResponse getDeliveryNoteByActivity(Long activityId) {
        DeliveryNote note = deliveryNoteRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Note not found for loading activity id: " + activityId));
        return toDeliveryNoteResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public TruckInvoiceResponse getTruckInvoiceByActivity(Long activityId) {
        TruckInvoice invoice = truckInvoiceRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Invoice not found for loading activity id: " + activityId));
        return toTruckInvoiceResponse(invoice);
    }

    @Override
    public DeliveryNoteResponse printDeliveryNote(Long noteId) {
        log.info("Printing delivery note with id: {}", noteId);
        DeliveryNote note = deliveryNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Note not found with id: " + noteId));

        if ("HANDED_TO_DRIVER".equals(note.getStatus())) {
            // A reprint must never regress a document that has been handed over.
            return toDeliveryNoteResponse(note);
        }
        if (!"PREPARED".equals(note.getStatus()) && !"PRINTED".equals(note.getStatus())) {
            throw new BadRequestException("Delivery Note cannot be printed from status " + note.getStatus() + ".");
        }

        String username = resolveCurrentUser();
        note.setStatus("PRINTED");
        note.setPrintedBy(username);
        note.setPrintedAt(LocalDateTime.now());
        note.setUpdatedBy(username);

        DeliveryNote saved = deliveryNoteRepository.save(note);
        return toDeliveryNoteResponse(saved);
    }

    @Override
    public TruckInvoiceResponse printTruckInvoice(Long invoiceId) {
        log.info("Printing truck invoice with id: {}", invoiceId);
        TruckInvoice invoice = truckInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Invoice not found with id: " + invoiceId));

        if (!"GENERATED".equals(invoice.getInvoiceStatus()) && !"PRINTED".equals(invoice.getInvoiceStatus())) {
            throw new BadRequestException("Truck Invoice cannot be printed from status " + invoice.getInvoiceStatus() + ".");
        }

        String username = resolveCurrentUser();
        invoice.setInvoiceStatus("PRINTED");
        invoice.setUpdatedBy(username);

        TruckInvoice saved = truckInvoiceRepository.save(invoice);
        return toTruckInvoiceResponse(saved);
    }

    @Override
    public DeliveryNoteResponse markHandedToDriver(Long noteId) {
        log.info("Marking delivery note handed to driver: {}", noteId);
        DeliveryNote note = deliveryNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Note not found with id: " + noteId));

        if (!"PRINTED".equals(note.getStatus()) && !"HANDED_TO_DRIVER".equals(note.getStatus())) {
            throw new BadRequestException("Delivery Note must be PRINTED before handing to driver.");
        }

        String username = resolveCurrentUser();
        note.setStatus("HANDED_TO_DRIVER");
        note.setUpdatedBy(username);

        DeliveryNote saved = deliveryNoteRepository.save(note);

        // Check if loading order can now move to READY_FOR_DISPATCH
        checkAndUpdateToReadyForDispatch(note.getLoadingOrder());

        return toDeliveryNoteResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryNoteResponse> getPendingDocumentationNotes() {
        return deliveryNoteRepository.findAll().stream()
                .map(this::toDeliveryNoteResponse)
                .collect(Collectors.toList());
    }

    private synchronized String generateDeliveryNoteNumber() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String settingPrefix = systemSettingService.getSetting("DELIVERY_NOTE_PREFIX", "DN-");
        String prefix = settingPrefix + dateStr + "-";
        String maxNumber = deliveryNoteRepository.findMaxDeliveryNoteNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "0001";
        }
        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%04d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max delivery note sequence: {}", maxNumber, e);
            return prefix + "0001";
        }
    }

    private synchronized String generateInvoiceNumber() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String settingPrefix = systemSettingService.getSetting("INVOICE_PREFIX", "INV-TRUCK-");
        String prefix = settingPrefix + dateStr + "-";
        String maxNumber = truckInvoiceRepository.findMaxInvoiceNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "0001";
        }
        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%04d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max invoice sequence: {}", maxNumber, e);
            return prefix + "0001";
        }
    }

    private void checkAndUpdateToDocumentsReady(LoadingOrder loadingOrder) {
        if (loadingOrder == null) return;
        List<LoadingActivity> activities = loadingActivityRepository.findByLoadingOrderId(loadingOrder.getId());
        List<LoadingActivity> requiredActivities = activities.stream()
                .filter(act -> act.getStatus() != LoadingActivityStatus.CANCELLED)
                .collect(Collectors.toList());
        boolean allHaveDocs = !requiredActivities.isEmpty()
                && requiredActivities.stream().allMatch(act ->
                        act.getStatus() == LoadingActivityStatus.COMPLETED
                                && deliveryNoteRepository.existsByLoadingActivityId(act.getId())
                                && truckInvoiceRepository.existsByLoadingActivityId(act.getId()));

        if (allHaveDocs) {
            loadingOrder.setStatus(LoadingOrderStatus.DOCUMENTS_READY);
            FuelOrder order = loadingOrder.getOrder();
            if (order != null) {
                order.setOrderStatus("DOCUMENTS_READY");
                fuelOrderRepository.save(order);
            }
            loadingOrderRepository.save(loadingOrder);
        }
    }

    private void checkAndUpdateToReadyForDispatch(LoadingOrder loadingOrder) {
        if (loadingOrder == null) return;

        List<LoadingActivity> activities = loadingActivityRepository.findByLoadingOrderId(loadingOrder.getId());
        List<LoadingActivity> requiredActivities = activities.stream()
                .filter(act -> act.getStatus() != LoadingActivityStatus.CANCELLED)
                .collect(Collectors.toList());
        boolean allHandedToDriver = !requiredActivities.isEmpty()
                && requiredActivities.stream().allMatch(act -> {
                    if (act.getStatus() != LoadingActivityStatus.COMPLETED) return false;
                    var noteOpt = deliveryNoteRepository.findByLoadingActivityId(act.getId());
                    return noteOpt.isPresent() && "HANDED_TO_DRIVER".equals(noteOpt.get().getStatus());
                });

        if (allHandedToDriver) {
            loadingOrder.setStatus(LoadingOrderStatus.READY_FOR_DISPATCH);
            FuelOrder order = loadingOrder.getOrder();
            if (order != null) {
                order.setOrderStatus("READY_FOR_DISPATCH");
                fuelOrderRepository.save(order);
            }
            loadingOrderRepository.save(loadingOrder);
        }
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private DeliveryNoteResponse toDeliveryNoteResponse(DeliveryNote dn) {
        return DeliveryNoteResponse.builder()
                .id(dn.getId())
                .deliveryNoteNumber(dn.getDeliveryNoteNumber())
                .loadingOrderId(dn.getLoadingOrder() != null ? dn.getLoadingOrder().getId() : null)
                .loadingActivityId(dn.getLoadingActivity() != null ? dn.getLoadingActivity().getId() : null)
                .loadingReportId(dn.getLoadingReport() != null ? dn.getLoadingReport().getId() : null)
                .customerId(dn.getCustomer() != null ? dn.getCustomer().getId() : null)
                .customerName(dn.getCustomer() != null ? dn.getCustomer().getCompanyName() : null)
                .productId(dn.getProduct() != null ? dn.getProduct().getId() : null)
                .productName(dn.getProduct() != null ? dn.getProduct().getProductName() : null)
                .truckNumber(dn.getTruckNumber())
                .driverName(dn.getDriverName())
                .driverLicenseNumber(dn.getDriverLicenseNumber())
                .transportCompany(dn.getTransportCompany())
                .truckCapacity(dn.getTruckCapacity())
                .transportCharge(dn.getTransportCharge())
                .ambientVolume(dn.getAmbientVolume())
                .standardVolume(dn.getStandardVolume())
                .destination(dn.getDestination())
                .status(dn.getStatus())
                .preparedBy(dn.getPreparedBy())
                .preparedAt(dn.getPreparedAt())
                .printedBy(dn.getPrintedBy())
                .printedAt(dn.getPrintedAt())
                .createdAt(dn.getCreatedAt())
                .build();
    }

    private TruckInvoiceResponse toTruckInvoiceResponse(TruckInvoice ti) {
        return TruckInvoiceResponse.builder()
                .id(ti.getId())
                .invoiceNumber(ti.getInvoiceNumber())
                .loadingOrderId(ti.getLoadingOrder() != null ? ti.getLoadingOrder().getId() : null)
                .loadingActivityId(ti.getLoadingActivity() != null ? ti.getLoadingActivity().getId() : null)
                .deliveryNoteId(ti.getDeliveryNote() != null ? ti.getDeliveryNote().getId() : null)
                .deliveryNoteNumber(ti.getDeliveryNote() != null ? ti.getDeliveryNote().getDeliveryNoteNumber() : null)
                .customerId(ti.getCustomer() != null ? ti.getCustomer().getId() : null)
                .customerName(ti.getCustomer() != null ? ti.getCustomer().getCompanyName() : null)
                .productId(ti.getProduct() != null ? ti.getProduct().getId() : null)
                .productName(ti.getProduct() != null ? ti.getProduct().getProductName() : null)
                .truckNumber(ti.getTruckNumber())
                .driverName(ti.getDriverName())
                .transportCompany(ti.getTransportCompany())
                .truckCapacity(ti.getTruckCapacity())
                .transportCharge(ti.getTransportCharge())
                .quantity(ti.getQuantity())
                .unitPrice(ti.getUnitPrice())
                .totalAmount(ti.getTotalAmount())
                .paymentStatus(ti.getPaymentStatus())
                .invoiceStatus(ti.getInvoiceStatus())
                .createdAt(ti.getCreatedAt())
                .build();
    }
}
