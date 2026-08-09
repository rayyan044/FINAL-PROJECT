package com.falconenergy.service.impl;

import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.dto.DeliveryArrivalRequest;
import com.falconenergy.dto.DeliveryCompleteRequest;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.DeliveryMapper;
import com.falconenergy.repository.*;
import com.falconenergy.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DispatchRepository dispatchRepository;
    private final LoadingActivityRepository loadingActivityRepository;
    private final LoadingOrderRepository loadingOrderRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryMapper deliveryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getActiveDeliveries() {
        log.info("Fetching active deliveries");
        return deliveryRepository.findAll().stream()
                .filter(d -> d.getDeliveryStatus() != DeliveryStatus.COMPLETED 
                        && d.getDeliveryStatus() != DeliveryStatus.CANCELLED)
                .map(deliveryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryResponse createDelivery(Long dispatchId) {
        log.info("Creating delivery record for dispatch: {}", dispatchId);

        // Check if delivery already exists for this dispatchId to prevent duplicates
        var existingOpt = deliveryRepository.findByDispatchId(dispatchId);
        if (existingOpt.isPresent()) {
            log.info("Delivery already exists for dispatch {}, returning existing record", dispatchId);
            return deliveryMapper.toResponse(existingOpt.get());
        }

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found with id: " + dispatchId));

        if (dispatch.getDispatchStatus() != DispatchStatus.DISPATCHED && 
            dispatch.getDispatchStatus() != DispatchStatus.IN_TRANSIT) {
            throw new BadRequestException("Dispatch status must be DISPATCHED or IN_TRANSIT before creating a delivery. Current status: " + dispatch.getDispatchStatus());
        }

        // Verify driver is assigned
        var activity = dispatch.getLoadingActivity();
        if (activity == null || activity.getVehicle() == null || activity.getVehicle().getDriver() == null) {
            throw new BadRequestException("Cannot create delivery: no driver is assigned to this dispatch.");
        }

        String username = resolveCurrentUser();
        String deliveryNumber = generateDeliveryNumber();

        Delivery delivery = Delivery.builder()
                .deliveryNumber(deliveryNumber)
                .dispatch(dispatch)
                .loadingOrder(dispatch.getLoadingOrder())
                .loadingActivity(dispatch.getLoadingActivity())
                .deliveryNote(dispatch.getDeliveryNote())
                .truckInvoice(dispatch.getTruckInvoice())
                .truckNumber(dispatch.getTruckNumber())
                .driverName(dispatch.getDriverName())
                .transportCompany(dispatch.getTransportCompany())
                .destination(dispatch.getDestination())
                .deliveryStatus(DeliveryStatus.ASSIGNED)
                .build();

        delivery.setCreatedBy(username);
        delivery.setUpdatedBy(username);

        Delivery saved = deliveryRepository.save(delivery);
        return deliveryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + id));
        return deliveryMapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse markArrived(Long deliveryId, DeliveryArrivalRequest request) {
        log.info("Recording arrival for delivery: {}", deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));

        if (delivery.getDeliveryStatus() != DeliveryStatus.IN_TRANSIT) {
            throw new BadRequestException("Arrival can only be recorded if the delivery status is IN_TRANSIT. Current status: " + delivery.getDeliveryStatus());
        }

        String username = resolveCurrentUser();
        delivery.setDeliveryStatus(DeliveryStatus.ARRIVED_AT_DESTINATION);
        delivery.setArrivalTime(LocalDateTime.now());
        delivery.setReceivedBy(request != null ? request.getReceivedBy() : "operations");
        if (request != null && request.getRemarks() != null) {
            delivery.setRemarks(request.getRemarks());
        }
        delivery.setUpdatedBy(username);

        Delivery saved = deliveryRepository.save(delivery);
        return deliveryMapper.toResponse(saved);
    }

    @Override
    public DeliveryResponse completeDelivery(Long deliveryId, DeliveryCompleteRequest request) {
        log.info("Completing delivery: {}", deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new BadRequestException("Delivery can only be completed if the status is DELIVERED. Current status: " + delivery.getDeliveryStatus());
        }

        // Verify POD exists
        if (delivery.getPodPhotoPath() == null || delivery.getPodPhotoPath().isEmpty()) {
            throw new BadRequestException("Cannot complete delivery: Proof of Delivery has not been uploaded.");
        }

        String username = resolveCurrentUser();
        delivery.setDeliveryStatus(DeliveryStatus.COMPLETED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setCompletedBy(request != null ? request.getCompletedBy() : "operations");
        if (request != null && request.getRemarks() != null) {
            delivery.setRemarks(request.getRemarks());
        }
        delivery.setUpdatedBy(username);

        // Update LoadingActivity Status
        LoadingActivity activity = delivery.getLoadingActivity();
        if (activity != null) {
            activity.setStatus(LoadingActivityStatus.DELIVERED);
            loadingActivityRepository.save(activity);
            if (activity.getVehicle() != null) {
                activity.getVehicle().setCurrentStatus("AVAILABLE");
                vehicleRepository.save(activity.getVehicle());
            }

            // Check & Update LoadingOrder Status if ALL activities under it are now DELIVERED
            checkAndUpdateOrderStatus(activity.getLoadingOrder());
        }

        Delivery saved = deliveryRepository.save(delivery);
        return deliveryMapper.toResponse(saved);
    }

    @Override
    public DeliveryResponse cancelDelivery(Long deliveryId, String remarks) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));
        if (delivery.getDeliveryStatus() == DeliveryStatus.COMPLETED
                || delivery.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            throw new BadRequestException("A completed or cancelled delivery cannot be cancelled");
        }
        delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
        delivery.setRemarks(remarks);
        delivery.setUpdatedBy(resolveCurrentUser());
        return deliveryMapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveryHistory() {
        log.info("Fetching delivery history");
        return deliveryRepository.findAll().stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.COMPLETED 
                        || d.getDeliveryStatus() == DeliveryStatus.CANCELLED)
                .map(deliveryMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void checkAndUpdateOrderStatus(LoadingOrder loadingOrder) {
        if (loadingOrder == null) return;

        List<LoadingActivity> activities = loadingActivityRepository.findByLoadingOrderId(loadingOrder.getId());
        boolean allDelivered = activities.stream()
                .filter(act -> act.getStatus() != LoadingActivityStatus.CANCELLED)
                .allMatch(act -> act.getStatus() == LoadingActivityStatus.DELIVERED);

        if (allDelivered && !activities.isEmpty()) {
            loadingOrder.setStatus(LoadingOrderStatus.DELIVERED);
            loadingOrderRepository.save(loadingOrder);

            FuelOrder fuelOrder = loadingOrder.getOrder();
            if (fuelOrder != null) {
                fuelOrder.setOrderStatus("DELIVERED");
                fuelOrderRepository.save(fuelOrder);
            }
        }
    }

    private synchronized String generateDeliveryNumber() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "DEL-" + dateStr + "-";
        String maxNumber = deliveryRepository.findMaxDeliveryNumberWithPrefix(prefix);
        if (maxNumber == null) {
            return prefix + "0001";
        }
        try {
            String seqStr = maxNumber.substring(prefix.length());
            int seq = Integer.parseInt(seqStr);
            return prefix + String.format("%04d", seq + 1);
        } catch (Exception e) {
            log.error("Error parsing max delivery number sequence: {}", maxNumber, e);
            return prefix + "0001";
        }
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
