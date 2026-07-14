package com.falconenergy.service.impl;

import com.falconenergy.dto.LoadingOrderRequest;
import com.falconenergy.dto.LoadingOrderResponse;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.LoadingOrder;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.TruckNomination;
import com.falconenergy.entity.TruckNominationItem;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.LoadingOrderMapper;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.LoadingOrderRepository;
import com.falconenergy.repository.LoadingActivityRepository;
import com.falconenergy.repository.TruckNominationRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.LoadingOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoadingOrderServiceImpl implements LoadingOrderService {

    private final LoadingOrderRepository loadingOrderRepository;
    private final LoadingActivityRepository loadingActivityRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final TruckNominationRepository truckNominationRepository;
    private final LoadingOrderMapper loadingOrderMapper;
    private final AuditLogService auditLogService;

    @Override
    public LoadingOrderResponse createLoadingOrder(LoadingOrderRequest request) {
        FuelOrder order = fuelOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel Order not found with id: " + request.getOrderId()));

        // Verification 1: Order status must be READY_FOR_LOADING
        if (!"READY_FOR_LOADING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Customer Order status must be READY_FOR_LOADING to create a Loading Order.");
        }

        // Verification 2: Payment must be approved
        Invoice invoice = order.getInvoice();
        if (invoice == null || !"PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new BadRequestException("Finance must approve the invoice payment before creating a Loading Order.");
        }

        // Verification 3: Nomination must be APPROVED
        TruckNomination nomination = truckNominationRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BadRequestException("Truck Nomination must exist and be approved."));
        if (!"APPROVED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Truck Nomination status must be APPROVED to proceed.");
        }

        // Verification 4: Prevent duplicate Loading Orders
        if (loadingOrderRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("A Loading Order already exists for this customer order.");
        }

        // Generate Loading Order Number
        String orderNumber = generateLoadingOrderNumber();

        LoadingOrder loadingOrder = LoadingOrder.builder()
                .loadingOrderNumber(orderNumber)
                .order(order)
                .loadingDate(request.getLoadingDate() != null ? request.getLoadingDate() : LocalDate.now())
                .loadingTerminal(request.getLoadingTerminal())
                .consignee(request.getConsignee())
                .status("DRAFT")
                .preparedBy(resolveCurrentUser())
                .loadingRemarks(request.getLoadingRemarks())
                .vesselName(request.getVesselName())
                .operationsManager(request.getOperationsManager())
                .build();

        // Copy truck information from the Truck Nomination
        List<LoadingActivity> activities = new ArrayList<>();
        int index = 1;
        for (TruckNominationItem item : nomination.getItems()) {
            LoadingActivity activity = LoadingActivity.builder()
                    .loadingOrder(loadingOrder)
                    .truckNumber(item.getTruckNumber())
                    .trailerNumber(item.getTrailerNumber())
                    .driverName(item.getDriverName())
                    .driverLicenceNumber(item.getDriverLicenceNumber())
                    .driverPassport(item.getDriverPassport())
                    .transportCompany(item.getTransportCompany())
                    .destination(item.getDestination())
                    .product(order.getProduct().getProductName())
                    .allocatedQuantity(item.getAllocatedQuantity())
                    .status("WAITING")
                    .queueNumber("Q-" + String.format("%03d", index++))
                    .bayNumber("BAY-1") // default bay
                    .build();
            activities.add(activity);
        }
        loadingOrder.setActivities(activities);

        // Update FuelOrder status to LOADING_ORDER_CREATED
        String prevStatus = order.getOrderStatus();
        order.setOrderStatus("LOADING_ORDER_CREATED");
        fuelOrderRepository.save(order);

        LoadingOrder saved = loadingOrderRepository.save(loadingOrder);

        String username = resolveCurrentUser();
        auditLogService.log("LOADING_ORDER_CREATED", "LOADING_ORDER", saved.getId(), order.getCustomer().getCustomerCode(),
                "Loading Order " + saved.getLoadingOrderNumber() + " created in DRAFT status by " + username);
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevStatus + " to LOADING_ORDER_CREATED for order " + order.getOrderNumber(), prevStatus, "LOADING_ORDER_CREATED");

        return loadingOrderMapper.toResponse(saved);
    }

    @Override
    public LoadingOrderResponse updateLoadingOrder(Long id, LoadingOrderRequest request) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if (!"DRAFT".equalsIgnoreCase(loadingOrder.getStatus())) {
            throw new BadRequestException("Loading Order can only be modified in DRAFT status.");
        }

        loadingOrder.setLoadingDate(request.getLoadingDate() != null ? request.getLoadingDate() : loadingOrder.getLoadingDate());
        loadingOrder.setLoadingTerminal(request.getLoadingTerminal());
        loadingOrder.setConsignee(request.getConsignee());
        loadingOrder.setLoadingRemarks(request.getLoadingRemarks());
        loadingOrder.setVesselName(request.getVesselName());
        loadingOrder.setOperationsManager(request.getOperationsManager());

        // Update activities (trucks) if provided
        if (request.getActivities() != null) {
            List<LoadingActivity> oldActivities = new ArrayList<>(loadingOrder.getActivities());
            List<LoadingActivity> newActivities = new ArrayList<>();
            
            // Map existing activities by truck number
            java.util.Map<String, LoadingActivity> oldMap = oldActivities.stream()
                    .collect(Collectors.toMap(act -> act.getTruckNumber().trim().toUpperCase(), act -> act, (a, b) -> a));

            // Map incoming requests by truck number
            java.util.Map<String, com.falconenergy.dto.LoadingActivityRequest> incomingMap = request.getActivities().stream()
                    .collect(Collectors.toMap(act -> act.getTruckNumber().trim().toUpperCase(), act -> act, (a, b) -> a));

            int index = 1;
            String product = loadingOrder.getOrder().getProduct().getProductName();
            String customerCode = loadingOrder.getOrder().getCustomer().getCustomerCode();

            for (com.falconenergy.dto.LoadingActivityRequest actReq : request.getActivities()) {
                String truckKey = actReq.getTruckNumber().trim().toUpperCase();
                LoadingActivity activity;

                if (oldMap.containsKey(truckKey)) {
                    activity = oldMap.get(truckKey);
                    
                    BigDecimal oldQty = activity.getAllocatedQuantity();
                    activity.setTrailerNumber(actReq.getTrailerNumber());
                    activity.setDriverName(actReq.getDriverName());
                    activity.setDriverLicenceNumber(actReq.getDriverLicenceNumber());
                    activity.setDriverPassport(actReq.getDriverPassport());
                    activity.setTransportCompany(actReq.getTransportCompany());
                    activity.setDestination(actReq.getDestination());
                    activity.setAllocatedQuantity(actReq.getAllocatedQuantity());

                    if (oldQty.compareTo(actReq.getAllocatedQuantity()) != 0) {
                        auditLogService.log("QUANTITY_UPDATED", "LOADING_ORDER", loadingOrder.getId(), customerCode,
                                "Quantity updated for truck " + activity.getTruckNumber() + " in Loading Order " + loadingOrder.getLoadingOrderNumber() + " from " + oldQty + " to " + actReq.getAllocatedQuantity(),
                                oldQty.toString(), actReq.getAllocatedQuantity().toString());
                    }
                } else {
                    activity = LoadingActivity.builder()
                            .loadingOrder(loadingOrder)
                            .truckNumber(actReq.getTruckNumber())
                            .trailerNumber(actReq.getTrailerNumber())
                            .driverName(actReq.getDriverName())
                            .driverLicenceNumber(actReq.getDriverLicenceNumber())
                            .driverPassport(actReq.getDriverPassport())
                            .transportCompany(actReq.getTransportCompany())
                            .destination(actReq.getDestination())
                            .product(product)
                            .allocatedQuantity(actReq.getAllocatedQuantity())
                            .status("WAITING")
                            .queueNumber("Q-" + String.format("%03d", index))
                            .bayNumber("BAY-1")
                            .build();

                    auditLogService.log("TRUCK_ADDED", "LOADING_ORDER", loadingOrder.getId(), customerCode,
                            "Truck " + activity.getTruckNumber() + " added to Loading Order " + loadingOrder.getLoadingOrderNumber(),
                            null, activity.getAllocatedQuantity().toString());
                }
                newActivities.add(activity);
                index++;
            }

            for (LoadingActivity oldAct : oldActivities) {
                String truckKey = oldAct.getTruckNumber().trim().toUpperCase();
                if (!incomingMap.containsKey(truckKey)) {
                    auditLogService.log("TRUCK_REMOVED", "LOADING_ORDER", loadingOrder.getId(), customerCode,
                            "Truck " + oldAct.getTruckNumber() + " removed from Loading Order " + loadingOrder.getLoadingOrderNumber(),
                            oldAct.getAllocatedQuantity().toString(), null);
                }
            }

            loadingOrder.getActivities().clear();
            loadingOrder.getActivities().addAll(newActivities);
        }

        LoadingOrder saved = loadingOrderRepository.save(loadingOrder);

        String username = resolveCurrentUser();
        auditLogService.log("LOADING_ORDER_UPDATED", "LOADING_ORDER", saved.getId(), saved.getOrder().getCustomer().getCustomerCode(),
                "Loading Order " + saved.getLoadingOrderNumber() + " updated by " + username);

        return loadingOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoadingOrderResponse getLoadingOrderById(Long id) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));
        return loadingOrderMapper.toResponse(loadingOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public LoadingOrderResponse getLoadingOrderByOrderId(Long orderId) {
        LoadingOrder loadingOrder = loadingOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found for order id: " + orderId));
        return loadingOrderMapper.toResponse(loadingOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoadingOrderResponse> getAllLoadingOrders() {
        return loadingOrderRepository.findAll().stream()
                .map(loadingOrderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LoadingOrderResponse approveLoadingOrder(Long id) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if (!"DRAFT".equalsIgnoreCase(loadingOrder.getStatus())) {
            throw new BadRequestException("Only DRAFT Loading Orders can be approved.");
        }

        loadingOrder.setStatus("APPROVED");
        loadingOrder.setApprovedBy(resolveCurrentUser());

        FuelOrder order = loadingOrder.getOrder();
        String prevStatus = order.getOrderStatus();
        order.setOrderStatus("LOADING_ORDER_APPROVED");
        fuelOrderRepository.save(order);

        LoadingOrder saved = loadingOrderRepository.save(loadingOrder);

        String username = resolveCurrentUser();
        auditLogService.log("LOADING_ORDER_APPROVED", "LOADING_ORDER", saved.getId(), order.getCustomer().getCustomerCode(),
                "Loading Order " + saved.getLoadingOrderNumber() + " approved and locked by " + username);
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevStatus + " to LOADING_ORDER_APPROVED for order " + order.getOrderNumber());

        return loadingOrderMapper.toResponse(saved);
    }

    @Override
    public LoadingOrderResponse cancelLoadingOrder(Long id) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if ("COMPLETED".equalsIgnoreCase(loadingOrder.getStatus()) || "CANCELLED".equalsIgnoreCase(loadingOrder.getStatus())) {
            throw new BadRequestException("Cannot cancel a completed or already cancelled Loading Order.");
        }

        String oldStatus = loadingOrder.getStatus();
        loadingOrder.setStatus("CANCELLED");

        FuelOrder order = loadingOrder.getOrder();
        String prevStatus = order.getOrderStatus();
        order.setOrderStatus("READY_FOR_LOADING");
        fuelOrderRepository.save(order);

        LoadingOrder saved = loadingOrderRepository.save(loadingOrder);

        String username = resolveCurrentUser();
        auditLogService.log("LOADING_ORDER_CANCELLED", "LOADING_ORDER", saved.getId(), order.getCustomer().getCustomerCode(),
                "Loading Order " + saved.getLoadingOrderNumber() + " cancelled by " + username + " (previous status: " + oldStatus + ")");
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status reset from " + prevStatus + " to READY_FOR_LOADING for order " + order.getOrderNumber());

        return loadingOrderMapper.toResponse(saved);
    }

    @Override
    public LoadingOrderResponse startLoadingActivity(Long id, Long activityId, String bayNumber, String pumpNumber) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if (!"APPROVED".equalsIgnoreCase(loadingOrder.getStatus()) && !"LOADING_IN_PROGRESS".equalsIgnoreCase(loadingOrder.getStatus())) {
            throw new BadRequestException("Loading cannot begin until the Loading Order has been approved.");
        }

        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Activity not found with id: " + activityId));

        if (!activity.getLoadingOrder().getId().equals(loadingOrder.getId())) {
            throw new BadRequestException("Activity does not belong to this loading order.");
        }

        if (!"WAITING".equalsIgnoreCase(activity.getStatus())) {
            throw new BadRequestException("Truck loading has already started or completed.");
        }

        if (bayNumber != null && !bayNumber.trim().isEmpty()) {
            activity.setBayNumber(bayNumber);
        }
        if (pumpNumber != null && !pumpNumber.trim().isEmpty()) {
            activity.setPumpNumber(pumpNumber);
        }

        String username = resolveCurrentUser();
        activity.setStatus("LOADING");
        activity.setLoadingStartTime(LocalDateTime.now());
        activity.setLoadingOfficer(username);
        loadingActivityRepository.save(activity);

        // Update Loading Order status to LOADING_IN_PROGRESS if it was APPROVED
        if ("APPROVED".equalsIgnoreCase(loadingOrder.getStatus())) {
            loadingOrder.setStatus("LOADING_IN_PROGRESS");
            FuelOrder order = loadingOrder.getOrder();
            String prevStatus = order.getOrderStatus();
            order.setOrderStatus("LOADING_IN_PROGRESS");
            fuelOrderRepository.save(order);
            loadingOrderRepository.save(loadingOrder);

            auditLogService.log("LOADING_ORDER_IN_PROGRESS", "LOADING_ORDER", loadingOrder.getId(), order.getCustomer().getCustomerCode(),
                    "Loading Order " + loadingOrder.getLoadingOrderNumber() + " status set to LOADING_IN_PROGRESS");
            auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                    "Order status changed from " + prevStatus + " to LOADING_IN_PROGRESS for order " + order.getOrderNumber());
        }

        auditLogService.log("LOADING_STARTED", "LOADING_ACTIVITY", activity.getId(), loadingOrder.getOrder().getCustomer().getCustomerCode(),
                "Loading started for truck " + activity.getTruckNumber() + " in Loading Order " + loadingOrder.getLoadingOrderNumber() + " by officer " + username);

        return loadingOrderMapper.toResponse(loadingOrder);
    }

    @Override
    public LoadingOrderResponse completeLoadingActivity(Long id, Long activityId) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Activity not found with id: " + activityId));

        if (!activity.getLoadingOrder().getId().equals(loadingOrder.getId())) {
            throw new BadRequestException("Activity does not belong to this loading order.");
        }

        if (!"LOADING".equalsIgnoreCase(activity.getStatus())) {
            throw new BadRequestException("Truck must be in LOADING status to complete loading.");
        }

        activity.setStatus("LOADED");
        activity.setLoadingCompletionTime(LocalDateTime.now());
        loadingActivityRepository.save(activity);

        auditLogService.log("LOADING_COMPLETED", "LOADING_ACTIVITY", activity.getId(), loadingOrder.getOrder().getCustomer().getCustomerCode(),
                "Loading completed for truck " + activity.getTruckNumber() + " in Loading Order " + loadingOrder.getLoadingOrderNumber());

        // Check if all trucks are loaded
        boolean allLoaded = loadingOrder.getActivities().stream()
                .allMatch(act -> "LOADED".equalsIgnoreCase(act.getStatus()));

        if (allLoaded) {
            loadingOrder.setStatus("COMPLETED");
            FuelOrder order = loadingOrder.getOrder();
            String prevStatus = order.getOrderStatus();
            order.setOrderStatus("LOADING_COMPLETED");
            fuelOrderRepository.save(order);
            loadingOrderRepository.save(loadingOrder);

            auditLogService.log("LOADING_ORDER_COMPLETED", "LOADING_ORDER", loadingOrder.getId(), order.getCustomer().getCustomerCode(),
                    "Loading Order " + loadingOrder.getLoadingOrderNumber() + " completed as all trucks are loaded.");
            auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                    "Order status changed from " + prevStatus + " to LOADING_COMPLETED for order " + order.getOrderNumber());
        }

        return loadingOrderMapper.toResponse(loadingOrder);
    }

    private synchronized String generateLoadingOrderNumber() {
        String yearStr = String.valueOf(LocalDate.now().getYear());
        String prefix = "LO-" + yearStr + "-";

        String maxNumber = loadingOrderRepository.findMaxOrderNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "000001";
        }

        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%06d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max loading order number sequence: {}", maxNumber, e);
            return prefix + "000001";
        }
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
