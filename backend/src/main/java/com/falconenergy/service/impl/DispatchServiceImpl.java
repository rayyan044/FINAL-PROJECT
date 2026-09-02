package com.falconenergy.service.impl;

import com.falconenergy.dto.DispatchResponse;
import com.falconenergy.dto.DispatchRequest;
import com.falconenergy.dto.LoadingActivityResponse;
import com.falconenergy.dto.LoadingReportResponse;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.*;
import com.falconenergy.service.DispatchService;
import com.falconenergy.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.falconenergy.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    @Lazy
    private DeliveryService deliveryService;

    private final DispatchRepository dispatchRepository;
    private final LoadingActivityRepository loadingActivityRepository;
    private final LoadingReportRepository loadingReportRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final TruckInvoiceRepository truckInvoiceRepository;
    private final LoadingOrderRepository loadingOrderRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final AuditLogService auditLogService;
    
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DeliveryRepository deliveryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LoadingActivityResponse> getPendingDispatchActivities() {
        log.info("Fetching activities pending dispatch");
        List<LoadingActivity> activities = loadingActivityRepository.findAll();

        return activities.stream()
                .filter(act -> act.getStatus() == LoadingActivityStatus.COMPLETED)
                .filter(act -> loadingReportRepository.findFirstByLoadingActivityIdOrderByCreatedAtDesc(act.getId()).isPresent())
                .filter(act -> deliveryNoteRepository.findByLoadingActivityId(act.getId())
                        .map(note -> "HANDED_TO_DRIVER".equals(note.getStatus()))
                        .orElse(false))
                .filter(act -> truckInvoiceRepository.existsByLoadingActivityId(act.getId()))
                .filter(act -> !dispatchRepository.existsByLoadingActivityId(act.getId()))
                .map(this::toLoadingActivityResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DispatchResponse createReadyDispatchForCompletedLoading(LoadingActivity activity, LoadingReport report) {
        if (activity.getStatus() != LoadingActivityStatus.COMPLETED) {
            throw new BadRequestException("A detailed loading activity must be COMPLETED before creating a Dispatch.");
        }
        if (report.getReportStatus() != LoadingReportStatus.GENERATED) {
            throw new BadRequestException("A completed Loading Report is required before creating a Dispatch.");
        }
        LoadingOrder loadingOrder = activity.getLoadingOrder();
        if (!deliveryNoteRepository.existsByLoadingActivityId(activity.getId())) {
            throw new BadRequestException("Delivery Note is required before creating a Dispatch.");
        }
        if (dispatchRepository.existsByLoadingActivityId(activity.getId())) {
            return toDispatchResponse(dispatchRepository.findByLoadingActivityId(activity.getId()).orElseThrow());
        }

        String username = resolveCurrentUser();
        Dispatch dispatch = Dispatch.builder()
                .dispatchNumber(generateDispatchNumber())
                .loadingOrder(loadingOrder)
                .loadingActivity(activity)
                .truckNumber(activity.getTruckNumber())
                .driverName(activity.getDriverName())
                .driverLicenseNumber(activity.getDriverLicenceNumber())
                .transportCompany(activity.getTransportCompany())
                .destination(activity.getDestination())
                .dispatchStatus(DispatchStatus.READY)
                .remarks("Automatically created after loading report " + report.getReportNumber() + " was generated.")
                .build();
        dispatch.setCreatedBy(username);
        dispatch.setUpdatedBy(username);
        Dispatch saved = dispatchRepository.save(dispatch);
        auditLogService.log("DISPATCH_CREATED", "DISPATCH", saved.getId(), username, "Dispatch created automatically after Loading Report " + report.getReportNumber() + " and all release documents were confirmed.", null, DispatchStatus.READY.name());
        return toDispatchResponse(saved);
    }

    @Override
    public DispatchResponse createDispatch(Long loadingActivityId, DispatchRequest request) {
        log.info("Creating dispatch record for activity: {}", loadingActivityId);

        LoadingActivity activity = loadingActivityRepository.findById(loadingActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading activity not found with id: " + loadingActivityId));

        if (activity.getStatus() != LoadingActivityStatus.COMPLETED) {
            throw new BadRequestException("Loading activity status must be COMPLETED before creating a Dispatch.");
        }

        LoadingReport report = loadingReportRepository.findFirstByLoadingActivityIdOrderByCreatedAtDesc(loadingActivityId)
                .orElseThrow(() -> new BadRequestException("Loading Report must exist."));
        if (report.getReportStatus() != LoadingReportStatus.GENERATED) {
            throw new BadRequestException("Loading Report must be generated before creating a Dispatch.");
        }

        DeliveryNote deliveryNote = deliveryNoteRepository.findByLoadingActivityId(loadingActivityId)
                .orElseThrow(() -> new BadRequestException("Delivery Note must exist."));

        if (!"HANDED_TO_DRIVER".equals(deliveryNote.getStatus())) {
            throw new BadRequestException("Delivery Note status must be HANDED_TO_DRIVER before creating a Dispatch.");
        }

        TruckInvoice truckInvoice = truckInvoiceRepository.findByLoadingActivityId(loadingActivityId)
                .orElseThrow(() -> new BadRequestException("Truck Invoice must exist."));

        if (dispatchRepository.existsByLoadingActivityId(loadingActivityId)) {
            throw new BadRequestException("A Dispatch record already exists for this loading activity.");
        }

        String username = resolveCurrentUser();
        String dispatchNumber = generateDispatchNumber();

        Dispatch dispatch = Dispatch.builder()
                .dispatchNumber(dispatchNumber)
                .loadingOrder(activity.getLoadingOrder())
                .loadingActivity(activity)
                .deliveryNote(deliveryNote)
                .truckInvoice(truckInvoice)
                .truckNumber(activity.getTruckNumber())
                .driverName(activity.getDriverName())
                .driverLicenseNumber(activity.getDriverLicenceNumber())
                .transportCompany(activity.getTransportCompany())
                .destination(activity.getDestination())
                .dispatchStatus(DispatchStatus.READY)
                .remarks(request != null ? request.getRemarks() : null)
                .build();

        dispatch.setCreatedBy(username);
        dispatch.setUpdatedBy(username);

        Dispatch saved = dispatchRepository.save(dispatch);
        return toDispatchResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchResponse getDispatchById(Long id) {
        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found with id: " + id));
        return toDispatchResponse(dispatch);
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchResponse getDispatchByActivityId(Long activityId) {
        Dispatch dispatch = dispatchRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found for activity id: " + activityId));
        return toDispatchResponse(dispatch);
    }

    @Override
    public DispatchResponse releaseTruck(Long id) {
        log.info("Releasing truck for dispatch: {}", id);
        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found with id: " + id));

        if (dispatch.getDispatchStatus() != DispatchStatus.READY) {
            throw new BadRequestException("Truck can only be released if the dispatch status is READY. Current status: " + dispatch.getDispatchStatus());
        }

        LoadingActivity releaseActivity = dispatch.getLoadingActivity();
        if (releaseActivity == null) {
            throw new BadRequestException("Truck cannot be released: the loading activity is missing.");
        }
        DeliveryNote deliveryNote = deliveryNoteRepository.findByLoadingActivityId(releaseActivity.getId())
                .orElseThrow(() -> new BadRequestException("Truck cannot be released: the Delivery Note has not been generated."));
        if (!"HANDED_TO_DRIVER".equals(deliveryNote.getStatus())) {
            throw new BadRequestException("Truck cannot be released: the Delivery Note must be handed to the driver first.");
        }
        LoadingOrder releaseOrder = dispatch.getLoadingOrder();
        Invoice customerInvoice = releaseOrder != null && releaseOrder.getOrder() != null ? releaseOrder.getOrder().getInvoice() : null;
        if (customerInvoice == null || !paymentReceiptRepository.existsByInvoiceId(customerInvoice.getId())) {
            throw new BadRequestException("Truck cannot be released: the Payment Receipt has not been generated.");
        }
        LoadingReport releaseReport = loadingReportRepository.findFirstByLoadingActivityIdOrderByCreatedAtDesc(releaseActivity.getId())
                .orElseThrow(() -> new BadRequestException("Truck cannot be released: the Loading Report is missing."));
        if (releaseReport.getReportStatus() != LoadingReportStatus.GENERATED) {
            throw new BadRequestException("Truck cannot be released: the Loading Report is not completed.");
        }
        Vehicle vehicle = releaseActivity.getVehicle();
        if (vehicle == null || !vehicle.isActive() || !"ASSIGNED".equalsIgnoreCase(vehicle.getCurrentStatus())) {
            throw new BadRequestException("Truck cannot be released: the nominated vehicle is not available for this dispatch.");
        }
        if (vehicle.getDriver() == null || !"AVAILABLE".equalsIgnoreCase(vehicle.getDriver().getStatus())) {
            throw new BadRequestException("Truck cannot be released: the nominated driver is not available.");
        }
        if (dispatch.getDeliveryNote() == null) {
            dispatch.setDeliveryNote(deliveryNoteRepository.findByLoadingActivityId(releaseActivity.getId()).orElseThrow());
        }

        String username = resolveCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        dispatch.setDispatchStatus(DispatchStatus.DISPATCHED);
        dispatch.setReleasedBy(username);
        dispatch.setReleasedAt(now);
        dispatch.setDispatchOfficer(username);
        dispatch.setDepartureTime(now);
        dispatch.setUpdatedBy(username);

        // Update LoadingActivity Status
        LoadingActivity activity = dispatch.getLoadingActivity();
        if (activity != null) {
            activity.setStatus(LoadingActivityStatus.DISPATCHED);
            loadingActivityRepository.save(activity);
            if (activity.getVehicle() != null) activity.getVehicle().setCurrentStatus("DISPATCHED");
            
            // Check & Update LoadingOrder Status
            checkAndUpdateOrderStatus(activity.getLoadingOrder(), LoadingActivityStatus.DISPATCHED, LoadingOrderStatus.DISPATCHED);
        }

        Dispatch saved = dispatchRepository.save(dispatch);
        auditLogService.log("TRUCK_RELEASED", "DISPATCH", saved.getId(), username, "Truck released for dispatch.", DispatchStatus.READY.name(), DispatchStatus.DISPATCHED.name());

        if (vehicle.getDriver() != null) {
            deliveryService.createDelivery(saved.getId());
        }

        return toDispatchResponse(saved);
    }

    @Override
    public DispatchResponse cancelDispatch(Long id, DispatchRequest request) {
        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found with id: " + id));

        if (dispatch.getDispatchStatus() != DispatchStatus.READY) {
            throw new BadRequestException("Only a READY dispatch can be cancelled. Current status: " + dispatch.getDispatchStatus());
        }

        String username = resolveCurrentUser();
        dispatch.setDispatchStatus(DispatchStatus.CANCELLED);
        dispatch.setRemarks(request != null && request.getRemarks() != null && !request.getRemarks().isBlank()
                ? request.getRemarks()
                : "Cancelled before truck release.");
        dispatch.setUpdatedBy(username);

        Dispatch saved = dispatchRepository.save(dispatch);
        auditLogService.log("DISPATCH_CANCELLED", "DISPATCH", saved.getId(), username,
                "Dispatch cancelled before truck release.", DispatchStatus.READY.name(), DispatchStatus.CANCELLED.name());

        deliveryRepository.findByDispatchId(saved.getId()).ifPresent(delivery -> {
            deliveryService.cancelDelivery(delivery.getId(), "Dispatch cancelled.");
        });

        var driver = dispatch.getLoadingActivity() != null && dispatch.getLoadingActivity().getVehicle() != null
                ? dispatch.getLoadingActivity().getVehicle().getDriver() : null;
        if (driver != null) {
            userRepository.findByDriverId(driver.getId()).ifPresent(user -> {
                notificationRepository.save(Notification.builder()
                        .user(user)
                        .title("Delivery Cancelled")
                        .message("Delivery for dispatch " + dispatch.getDispatchNumber() + " has been cancelled.")
                        .build());
            });
        }

        return toDispatchResponse(saved);
    }

    @Override
    public DispatchResponse startTransit(Long id) {
        log.info("Starting transit for dispatch: {}", id);
        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found with id: " + id));

        if (dispatch.getDispatchStatus() != DispatchStatus.DISPATCHED) {
            throw new BadRequestException("Transit can only be started if the truck is in DISPATCHED status. Current status: " + dispatch.getDispatchStatus());
        }

        String username = resolveCurrentUser();
        dispatch.setDispatchStatus(DispatchStatus.IN_TRANSIT);
        dispatch.setUpdatedBy(username);

        // Update LoadingActivity Status
        LoadingActivity activity = dispatch.getLoadingActivity();
        if (activity != null) {
            activity.setStatus(LoadingActivityStatus.IN_TRANSIT);
            loadingActivityRepository.save(activity);
            if (activity.getVehicle() != null) activity.getVehicle().setCurrentStatus("IN_TRANSIT");

            // Check & Update LoadingOrder Status
            checkAndUpdateOrderStatus(activity.getLoadingOrder(), LoadingActivityStatus.IN_TRANSIT, LoadingOrderStatus.IN_TRANSIT);
        }

        Dispatch saved = dispatchRepository.save(dispatch);
        auditLogService.log("TRANSIT_STARTED", "DISPATCH", saved.getId(), username, "Truck transit started.", DispatchStatus.DISPATCHED.name(), DispatchStatus.IN_TRANSIT.name());

        deliveryRepository.findByDispatchId(saved.getId()).ifPresentOrElse(delivery -> {
            delivery.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);
            delivery.setDispatchedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
        }, () -> {
            deliveryService.createDelivery(saved.getId());
            deliveryRepository.findByDispatchId(saved.getId()).ifPresent(delivery -> {
                delivery.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);
                delivery.setDispatchedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
            });
        });

        return toDispatchResponse(saved);
    }

    private void checkAndUpdateOrderStatus(LoadingOrder loadingOrder, LoadingActivityStatus activityTargetStatus, LoadingOrderStatus orderTargetStatus) {
        if (loadingOrder == null) return;

        List<LoadingActivity> activities = loadingActivityRepository.findByLoadingOrderId(loadingOrder.getId());
        boolean allMatch = activities.stream()
                .filter(act -> act.getStatus() != LoadingActivityStatus.CANCELLED)
                .allMatch(act -> act.getStatus() == activityTargetStatus);

        if (allMatch && !activities.isEmpty()) {
            loadingOrder.setStatus(orderTargetStatus);
            loadingOrderRepository.save(loadingOrder);
        }
    }

    private synchronized String generateDispatchNumber() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "DIS-" + dateStr + "-";
        String maxNumber = dispatchRepository.findMaxDispatchNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "0001";
        }
        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%04d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max dispatch number sequence: {}", maxNumber, e);
            return prefix + "0001";
        }
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private LoadingActivityResponse toLoadingActivityResponse(LoadingActivity act) {
        List<LoadingReportResponse> reportsList = act.getReports().stream()
                .map(rep -> LoadingReportResponse.builder()
                        .id(rep.getId())
                        .reportNumber(rep.getReportNumber())
                        .build())
                .collect(Collectors.toList());

        return LoadingActivityResponse.builder()
                .id(act.getId())
                .truckNumber(act.getTruckNumber())
                .trailerNumber(act.getTrailerNumber())
                .driverName(act.getDriverName())
                .driverLicenceNumber(act.getDriverLicenceNumber())
                .driverPassport(act.getDriverPassport())
                .transportCompany(act.getTransportCompany())
                .destination(act.getDestination())
                .product(act.getProduct())
                .allocatedQuantity(act.getAllocatedQuantity())
                .queueNumber(act.getQueueNumber())
                .bayNumber(act.getBayNumber())
                .pumpNumber(act.getPumpNumber())
                .loadingStartTime(act.getLoadingStartTime())
                .loadingCompletionTime(act.getLoadingCompletionTime())
                .loadingOfficer(act.getLoadingOfficer())
                .status(act.getStatus().name())
                .ambientVolume(act.getAmbientVolume())
                .temperature(act.getTemperature())
                .density(act.getDensity())
                .standardVolume(act.getStandardVolume())
                .remarks(act.getRemarks())
                .completedAt(act.getCompletedAt())
                .reports(reportsList)
                .build();
    }

    private DispatchResponse toDispatchResponse(Dispatch d) {
        return DispatchResponse.builder()
                .id(d.getId())
                .dispatchNumber(d.getDispatchNumber())
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
                .driverLicenseNumber(d.getDriverLicenseNumber())
                .transportCompany(d.getTransportCompany())
                .destination(d.getDestination())
                .dispatchOfficer(d.getDispatchOfficer())
                .departureTime(d.getDepartureTime())
                .releasedBy(d.getReleasedBy())
                .releasedAt(d.getReleasedAt())
                .dispatchStatus(d.getDispatchStatus().name())
                .remarks(d.getRemarks())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
