package com.falconenergy.service.impl;

import com.falconenergy.dto.LoadingOrderRequest;
import com.falconenergy.dto.LoadingOrderResponse;
import com.falconenergy.dto.LoadingActivityCompletionRequest;
import com.falconenergy.dto.LoadingReportResponse;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.LoadingOrder;
import com.falconenergy.entity.LoadingActivity;
import com.falconenergy.entity.LoadingActivityStatus;
import com.falconenergy.entity.LoadingOrderStatus;
import com.falconenergy.entity.InventoryMovementType;
import com.falconenergy.entity.LoadingReportStatus;
import com.falconenergy.entity.LoadingCompartment;
import com.falconenergy.entity.InventoryTransaction;
import com.falconenergy.entity.LoadingReport;
import com.falconenergy.entity.User;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.LoadingOrderMapper;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.LoadingOrderRepository;
import com.falconenergy.repository.LoadingActivityRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.repository.TruckPricingRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.LoadingCompartmentRepository;
import com.falconenergy.repository.InventoryTransactionRepository;
import com.falconenergy.repository.LoadingReportRepository;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.repository.OrderTruckAllocationRepository;
import com.falconenergy.entity.OrderTruckAllocation;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.LoadingOrderService;
import com.falconenergy.service.FleetAllocationService;
import com.falconenergy.service.TransportPriceRangeService;
import com.falconenergy.service.DispatchService;
import com.falconenergy.service.DeliveryDocumentService;
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
    private final VehicleRepository vehicleRepository;
    private final TruckPricingRepository truckPricingRepository;
    private final FleetAllocationService fleetAllocationService;
    private final LoadingOrderMapper loadingOrderMapper;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final FuelProductRepository fuelProductRepository;
    private final LoadingCompartmentRepository loadingCompartmentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final LoadingReportRepository loadingReportRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderTruckAllocationRepository orderTruckAllocationRepository;
    private final TransportPriceRangeService transportPriceRangeService;
    private final DispatchService dispatchService;
    private final DeliveryDocumentService deliveryDocumentService;

    @Override
    public LoadingOrderResponse createLoadingOrder(LoadingOrderRequest request) {
        FuelOrder order = fuelOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel Order not found with id: " + request.getOrderId()));

        // A payment-confirmed order is handed directly to Operations; Sales does not nominate trucks.
        if (!"PAYMENT_CONFIRMED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Customer Order must have payment confirmed before fleet allocation.");
        }

        // Verification 2: Payment must be approved
        Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
        if (invoice == null || !"PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new BadRequestException("Finance must approve the invoice payment before creating a Loading Order.");
        }
        if (invoice.getOrder() == null || !order.getId().equals(invoice.getOrder().getId())) {
            throw new BadRequestException("The selected Loading Order must use the invoice linked to its customer order.");
        }

        // Prevent duplicate Loading Orders
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
                .status(LoadingOrderStatus.DRAFT)
                .preparedBy(resolveCurrentUser())
                .loadingRemarks(request.getLoadingRemarks())
                .vesselName(request.getVesselName())
                .operationsManager(request.getOperationsManager())
                .build();

        // Reserve compatible company vehicles once payment is confirmed, then use those snapshots for loading.
        List<LoadingActivity> activities = new ArrayList<>();
        int index = 1;
        BigDecimal remaining = order.getApprovedQuantity() != null ? order.getApprovedQuantity() : order.getQuantity();
        List<OrderTruckAllocation> allocations = orderTruckAllocationRepository.findByOrderId(order.getId());
        if (allocations.isEmpty()) {
            allocations = allocateFleetForLoading(order);
        }
        for (OrderTruckAllocation allocation : allocations) {
            Vehicle vehicle = allocation.getVehicle();
            BigDecimal allocated = allocation.getAllocatedQuantity();
            BigDecimal transportCharge = allocation.getTransportPrice();
            LoadingActivity activity = LoadingActivity.builder()
                    .loadingOrder(loadingOrder)
                    .vehicle(vehicle)
                    .truckNumber(vehicle.getTruckNumber())
                    .driverName(vehicle.getDriver() == null ? "Unassigned" : vehicle.getDriver().getFirstName() + " " + vehicle.getDriver().getLastName())
                    .driverLicenceNumber(vehicle.getDriver() == null ? "Unassigned" : vehicle.getDriver().getLicenseNumber())
                    .transportCompany("FALCON ENERGY")
                    .destination(order.getDestination() != null ? order.getDestination() : request.getConsignee())
                    .product(order.getProduct().getProductName())
                    .allocatedQuantity(allocated)
                    .transportCharge(transportCharge)
                    .status(LoadingActivityStatus.PENDING)
                    .queueNumber("Q-" + String.format("%03d", index++))
                    .bayNumber("BAY-1") // default bay
                    .build();
            activities.add(activity);
            remaining = remaining.subtract(allocated);
            vehicle.setCurrentStatus("ASSIGNED");
            vehicleRepository.save(vehicle);
        }
        loadingOrder.setActivities(activities);
        updateTransportInvoice(order, activities);

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

    private List<OrderTruckAllocation> allocateFleetForLoading(FuelOrder order) {
        BigDecimal remaining = order.getApprovedQuantity() != null ? order.getApprovedQuantity() : order.getQuantity();
        BigDecimal totalTransport = BigDecimal.ZERO;
        List<OrderTruckAllocation> allocations = new ArrayList<>();

        for (Vehicle vehicle : fleetAllocationService.suggest(order)) {
            BigDecimal price = transportPriceRangeService.resolve(order.getProduct(), order.getQuantity());
            BigDecimal allocatedQuantity = remaining.min(vehicle.getCapacity());
            OrderTruckAllocation allocation = orderTruckAllocationRepository.save(OrderTruckAllocation.builder()
                    .order(order)
                    .vehicle(vehicle)
                    .allocatedQuantity(allocatedQuantity)
                    .capacitySnapshot(vehicle.getCapacity())
                    .transportPrice(price)
                    .build());
            allocations.add(allocation);
            remaining = remaining.subtract(allocatedQuantity);
            totalTransport = totalTransport.add(price);
            vehicle.setCurrentStatus("ASSIGNED");
            vehicleRepository.save(vehicle);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("The available fleet cannot satisfy this order quantity.");
        }

        order.setTransportCharges(totalTransport);
        fuelOrderRepository.save(order);
        auditLogService.log("TRUCK_AUTO_ASSIGNED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Company fleet allocated for loading order " + order.getOrderNumber() + "; transport charge " + totalTransport);
        return allocations;
    }

    @Override
    public LoadingOrderResponse updateLoadingOrder(Long id, LoadingOrderRequest request) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if (loadingOrder.getStatus() != LoadingOrderStatus.DRAFT) {
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
                            .status(LoadingActivityStatus.PENDING)
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

        if (loadingOrder.getStatus() != LoadingOrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT Loading Orders can be approved.");
        }

        loadingOrder.setStatus(LoadingOrderStatus.APPROVED);
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

        if (loadingOrder.getStatus() == LoadingOrderStatus.COMPLETED || loadingOrder.getStatus() == LoadingOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel a completed or already cancelled Loading Order.");
        }

        LoadingOrderStatus oldStatus = loadingOrder.getStatus();
        loadingOrder.setStatus(LoadingOrderStatus.CANCELLED);
        for (LoadingActivity activity : loadingOrder.getActivities()) {
            if (activity.getVehicle() != null) {
                activity.getVehicle().setCurrentStatus("AVAILABLE");
                vehicleRepository.save(activity.getVehicle());
            }
        }

        FuelOrder order = loadingOrder.getOrder();
        String prevStatus = order.getOrderStatus();
        order.setOrderStatus("PAYMENT_CONFIRMED");
        fuelOrderRepository.save(order);

        LoadingOrder saved = loadingOrderRepository.save(loadingOrder);

        String username = resolveCurrentUser();
        auditLogService.log("LOADING_ORDER_CANCELLED", "LOADING_ORDER", saved.getId(), order.getCustomer().getCustomerCode(),
                "Loading Order " + saved.getLoadingOrderNumber() + " cancelled by " + username + " (previous status: " + oldStatus.name() + ")");
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status reset from " + prevStatus + " to PAYMENT_CONFIRMED for order " + order.getOrderNumber());

        return loadingOrderMapper.toResponse(saved);
    }

    /** Stores the selected-rate snapshot on the invoice so future price updates never alter history. */
    private void updateTransportInvoice(FuelOrder order, List<LoadingActivity> activities) {
        BigDecimal transport = activities.stream().map(LoadingActivity::getTransportCharge)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTransportCharges(transport);
        fuelOrderRepository.save(order);
        Invoice invoice = order.getInvoice();
        if (invoice == null) return;
        BigDecimal fuel = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        BigDecimal levies = order.getLevies() == null ? BigDecimal.ZERO : order.getLevies();
        BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
        BigDecimal delivery = order.getDeliveryCharges() == null ? BigDecimal.ZERO : order.getDeliveryCharges();
        BigDecimal additional = order.getAdditionalCharges() == null ? BigDecimal.ZERO : order.getAdditionalCharges();
        BigDecimal subtotal = fuel.add(transport).add(levies).add(delivery).add(additional).subtract(discount)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        invoice.setTransportCharges(transport);
        invoice.setSubtotal(subtotal);
        invoice.setTax(subtotal.multiply(new BigDecimal("0.16")).setScale(2, java.math.RoundingMode.HALF_UP));
        invoice.setGrandTotal(subtotal.add(invoice.getTax()));
        invoiceRepository.save(invoice);
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

    @Override
    public LoadingOrderResponse startLoadingActivity(Long id, Long activityId, String bayNumber, String pumpNumber) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        if (loadingOrder.getStatus() != LoadingOrderStatus.APPROVED && loadingOrder.getStatus() != LoadingOrderStatus.LOADING_IN_PROGRESS) {
            throw new BadRequestException("Loading cannot begin until the Loading Order has been approved.");
        }

        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Activity not found with id: " + activityId));

        if (!activity.getLoadingOrder().getId().equals(loadingOrder.getId())) {
            throw new BadRequestException("Activity does not belong to this loading order.");
        }

        if (activity.getStatus() != LoadingActivityStatus.PENDING) {
            throw new BadRequestException("Truck loading has already started or completed.");
        }

        if (bayNumber != null && !bayNumber.trim().isEmpty()) {
            activity.setBayNumber(bayNumber);
        }
        if (pumpNumber != null && !pumpNumber.trim().isEmpty()) {
            activity.setPumpNumber(pumpNumber);
        }

        String username = resolveCurrentUser();
        activity.setStatus(LoadingActivityStatus.STARTED);
        activity.setLoadingStartTime(LocalDateTime.now());
        activity.setLoadingOfficer(username);
        loadingActivityRepository.save(activity);

        // Update Loading Order status to LOADING_IN_PROGRESS if it was APPROVED
        if (loadingOrder.getStatus() == LoadingOrderStatus.APPROVED) {
            loadingOrder.setStatus(LoadingOrderStatus.LOADING_IN_PROGRESS);
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
    public LoadingOrderResponse completeLoadingActivity(Long id, Long activityId, LoadingActivityCompletionRequest request) {
        LoadingOrder loadingOrder = loadingOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Order not found with id: " + id));

        LoadingActivity activity = loadingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Activity not found with id: " + activityId));

        if (!activity.getLoadingOrder().getId().equals(loadingOrder.getId())) {
            throw new BadRequestException("Activity does not belong to this loading order.");
        }

        if (activity.getStatus() == LoadingActivityStatus.COMPLETED) {
            throw new BadRequestException("Loading activity is already completed and locked.");
        }

        if (request.getCompartments() == null || request.getCompartments().isEmpty()) {
            throw new BadRequestException("At least one compartment entry is required.");
        }

        BigDecimal meterStart = request.getMeterStart() != null ? request.getMeterStart() : BigDecimal.ZERO;
        BigDecimal meterEnd = request.getMeterEnd() != null ? request.getMeterEnd() : BigDecimal.ZERO;
        if (meterEnd.compareTo(meterStart) < 0) {
            throw new BadRequestException("Meter end reading cannot be less than meter start reading.");
        }
        BigDecimal meterDifference = meterEnd.subtract(meterStart);

        if (request.getBayNumber() != null && !request.getBayNumber().trim().isEmpty()) {
            activity.setBayNumber(request.getBayNumber());
        }
        if (request.getPumpNumber() != null && !request.getPumpNumber().trim().isEmpty()) {
            activity.setPumpNumber(request.getPumpNumber());
        }

        User currentUser = resolveCurrentUserUser();
        String username = currentUser != null ? currentUser.getUsername() : "system";

        BigDecimal totalAmbient = BigDecimal.ZERO;
        BigDecimal totalStandard = BigDecimal.ZERO;
        BigDecimal sumTemp = BigDecimal.ZERO;
        BigDecimal sumDensity = BigDecimal.ZERO;

        List<LoadingCompartment> compartmentsToSave = new ArrayList<>();

        for (com.falconenergy.dto.LoadingCompartmentRequest req : request.getCompartments()) {
            if (req.getTemperature().compareTo(BigDecimal.ZERO) < 0 || req.getTemperature().compareTo(new BigDecimal("60")) > 0) {
                throw new BadRequestException("Observed temperature " + req.getTemperature() + "°C is outside valid range (0°C to 60°C).");
            }
            if (req.getAmbientVolume().compareTo(req.getCapacity()) > 0) {
                throw new BadRequestException("Ambient volume " + req.getAmbientVolume() + " exceeds compartment capacity " + req.getCapacity() + ".");
            }

            FuelProduct product = fuelProductRepository.findById(req.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + req.getProductId()));

            // Formula: V_std = V_amb * [1 - alpha * (T_obs - 20)]
            BigDecimal alpha = product.getThermalExpansionCoefficient();
            BigDecimal tempDiff = req.getTemperature().subtract(new BigDecimal("20"));
            BigDecimal correction = BigDecimal.ONE.subtract(alpha.multiply(tempDiff));
            BigDecimal standardVolume = req.getAmbientVolume().multiply(correction).setScale(2, java.math.RoundingMode.HALF_UP);

            LoadingCompartment compartment = LoadingCompartment.builder()
                    .loadingActivity(activity)
                    .compartmentNumber(req.getCompartmentNumber())
                    .capacity(req.getCapacity())
                    .product(product)
                    .productNameSnapshot(product.getProductName())
                    .productCodeSnapshot(product.getFuelCategory()) // using category as a fallback code if code doesn't exist
                    .ambientVolume(req.getAmbientVolume())
                    .temperature(req.getTemperature())
                    .density(req.getDensity())
                    .standardVolume(standardVolume)
                    .sealNumber(req.getSealNumber() != null ? req.getSealNumber() : "N/A")
                    .build();

            compartmentsToSave.add(compartment);

            totalAmbient = totalAmbient.add(req.getAmbientVolume());
            totalStandard = totalStandard.add(standardVolume);
            sumTemp = sumTemp.add(req.getTemperature());
            sumDensity = sumDensity.add(req.getDensity());
        }

        // Validations
        if (totalStandard.compareTo(activity.getAllocatedQuantity()) > 0) {
            throw new BadRequestException("Loaded standard volume (" + totalStandard + ") exceeds allocated quantity (" + activity.getAllocatedQuantity() + ").");
        }

        // Meter difference vs ambient volume validation (within 0.5% tolerance)
        BigDecimal difference = totalAmbient.subtract(meterDifference).abs();
        BigDecimal tolerance = meterDifference.multiply(new BigDecimal("0.005"));
        if (difference.compareTo(tolerance) > 0) {
            throw new BadRequestException("Meter difference (" + meterDifference + ") does not match sum of compartment ambient volumes (" + totalAmbient + ") within 0.5% tolerance.");
        }

        // Concurrency-locked stock updates and inventory logging
        for (LoadingCompartment compartment : compartmentsToSave) {
            FuelProduct lockedProduct = fuelProductRepository.findByIdWithLock(compartment.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + compartment.getProduct().getId()));

            BigDecimal stockBefore = lockedProduct.getAvailableQuantity();
            BigDecimal stockAfter = stockBefore.subtract(compartment.getStandardVolume());
            if (stockAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Insufficient inventory in stock for product: " + lockedProduct.getProductName() + ". Available: " + stockBefore);
            }

            lockedProduct.setAvailableQuantity(stockAfter);
            fuelProductRepository.save(lockedProduct);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .product(lockedProduct)
                    .quantity(compartment.getStandardVolume())
                    .stockBefore(stockBefore)
                    .stockAfter(stockAfter)
                    .movementType(InventoryMovementType.LOADING)
                    .movementReason("Loaded onto truck " + activity.getTruckNumber() + ", Loading Order " + loadingOrder.getLoadingOrderNumber())
                    .performedBy(currentUser)
                    .referenceId(activity.getId())
                    .referenceType("LOADING_ACTIVITY")
                    .build();
            inventoryTransactionRepository.save(transaction);
        }

        // Persist compartments
        loadingCompartmentRepository.saveAll(compartmentsToSave);
        activity.getCompartments().clear();
        activity.getCompartments().addAll(compartmentsToSave);

        BigDecimal countComp = new BigDecimal(compartmentsToSave.size());
        BigDecimal avgTemp = sumTemp.divide(countComp, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal avgDensity = sumDensity.divide(countComp, 4, java.math.RoundingMode.HALF_UP);

        // Update Loading Activity status and metadata
        activity.setStatus(LoadingActivityStatus.COMPLETED);
        activity.setAmbientVolume(totalAmbient);
        activity.setStandardVolume(totalStandard);
        activity.setTemperature(avgTemp);
        activity.setDensity(avgDensity);
        activity.setMeterStart(meterStart);
        activity.setMeterEnd(meterEnd);
        activity.setMeterDifference(meterDifference);
        activity.setRemarks(request.getRemarks());
        activity.setCompletedBy(currentUser);
        activity.setCompletedAt(LocalDateTime.now());
        activity.setLoadingCompletionTime(LocalDateTime.now());
        loadingActivityRepository.save(activity);

        // Generate Loading Report automatically
        String reportNumber = generateReportNumber();
        LoadingReport report = LoadingReport.builder()
                .loadingActivity(activity)
                .loadingOrder(loadingOrder)
                .reportNumber(reportNumber)
                .loadingOfficer(username)
                .terminal(loadingOrder.getLoadingTerminal())
                .loadingBay(activity.getBayNumber())
                .reportStatus(LoadingReportStatus.GENERATED)
                .build();
        loadingReportRepository.save(report);
        activity.getReports().add(report);
        deliveryDocumentService.generateDocumentsForCompletedLoading(activity, report);
        dispatchService.createReadyDispatchForCompletedLoading(activity, report);

        // Check if all activities for this loading order are COMPLETED
        boolean allCompleted = loadingOrder.getActivities().stream()
                .allMatch(act -> act.getStatus() == LoadingActivityStatus.COMPLETED);

        if (allCompleted) {
            loadingOrder.setStatus(LoadingOrderStatus.DOCUMENTATION_PENDING);
            FuelOrder order = loadingOrder.getOrder();
            String prevStatus = order.getOrderStatus();
            order.setOrderStatus("DOCUMENTATION_PENDING");
            fuelOrderRepository.save(order);
            loadingOrderRepository.save(loadingOrder);

            auditLogService.log("LOADING_ORDER_COMPLETED", "LOADING_ORDER", loadingOrder.getId(), order.getCustomer().getCustomerCode(),
                    "Loading Order " + loadingOrder.getLoadingOrderNumber() + " status set to DOCUMENTATION_PENDING as all trucks are completed.");
            auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                    "Order status changed from " + prevStatus + " to DOCUMENTATION_PENDING for order " + order.getOrderNumber());
        }

        auditLogService.log("LOADING_COMPLETED", "LOADING_ACTIVITY", activity.getId(), loadingOrder.getOrder().getCustomer().getCustomerCode(),
                "Loading completed for truck " + activity.getTruckNumber() + " in Loading Order " + loadingOrder.getLoadingOrderNumber() + " by officer " + username);

        return loadingOrderMapper.toResponse(loadingOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public LoadingReportResponse getLoadingReportByActivityId(Long activityId) {
        LoadingReport report = loadingReportRepository.findByLoadingActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Loading Report not found for activity id: " + activityId));
        return loadingOrderMapper.toReportResponse(report);
    }

    private synchronized String generateReportNumber() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "LR-" + dateStr + "-";

        String maxNumber = loadingReportRepository.findMaxReportNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "0001";
        }

        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%04d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max report number sequence: {}", maxNumber, e);
            return prefix + "0001";
        }
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private User resolveCurrentUserUser() {
        String username = resolveCurrentUser();
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByUsername(username))
                .orElse(null);
    }
}
