package com.falconenergy.service.impl;

import com.falconenergy.dto.TruckNominationItemDto;
import com.falconenergy.dto.TruckNominationRequest;
import com.falconenergy.dto.TruckNominationResponse;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.TruckNomination;
import com.falconenergy.entity.TruckNominationItem;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.TruckNominationMapper;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.repository.TruckNominationRepository;
import com.falconenergy.repository.TruckNominationItemRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.TruckNominationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TruckNominationServiceImpl implements TruckNominationService {

    private final TruckNominationRepository truckNominationRepository;
    private final TruckNominationItemRepository truckNominationItemRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final TruckNominationMapper truckNominationMapper;
    private final AuditLogService auditLogService;

    @Override
    public TruckNominationResponse createNominationDraft(TruckNominationRequest request) {
        FuelOrder order = fuelOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel Order not found with id: " + request.getOrderId()));

        // Check if invoice exists and is PAID
        Invoice invoice = order.getInvoice();
        if (invoice == null || !"PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new BadRequestException("Truck Nomination is only available after Finance has approved payment.");
        }

        // Check if a nomination already exists
        if (truckNominationRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("Truck Nomination already exists for order " + order.getOrderNumber());
        }

        // Create draft nomination
        TruckNomination nomination = TruckNomination.builder()
                .order(order)
                .transportSource(request.getTransportSource())
                .numberOfTrucks(request.getNumberOfTrucks())
                .confirmationNotes(request.getConfirmationNotes())
                .status("DRAFT")
                .build();

        validateAndPopulateItems(nomination, request.getItems(), order);

        // Update FuelOrder status to TRUCK_NOMINATION_DRAFT
        String prevStatus = order.getOrderStatus();
        order.setOrderStatus("TRUCK_NOMINATION_DRAFT");
        fuelOrderRepository.save(order);

        TruckNomination saved = truckNominationRepository.save(nomination);

        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_CREATED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination Created in DRAFT status for order " + order.getOrderNumber() + " by " + username);
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevStatus + " to TRUCK_NOMINATION_DRAFT for order " + order.getOrderNumber(), prevStatus, "TRUCK_NOMINATION_DRAFT");

        // Log initial trucks added
        for (TruckNominationItem item : saved.getItems()) {
            auditLogService.log("TRUCK_ADDED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                    "Truck " + item.getTruckNumber() + " added to nomination with quantity " + item.getAllocatedQuantity(),
                    null, item.getAllocatedQuantity().toString());
        }

        return truckNominationMapper.toResponse(saved);
    }

    @Override
    public TruckNominationResponse updateNominationDraft(Long id, TruckNominationRequest request) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));

        if ("SUBMITTED".equalsIgnoreCase(nomination.getStatus()) || "APPROVED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Cannot update nomination: it has already been submitted or approved.");
        }

        FuelOrder order = nomination.getOrder();

        // Capture previous items to compare
        List<TruckNominationItem> oldItems = new ArrayList<>(nomination.getItems());

        // Update details
        nomination.setTransportSource(request.getTransportSource());
        nomination.setNumberOfTrucks(request.getNumberOfTrucks());
        nomination.setConfirmationNotes(request.getConfirmationNotes());

        validateAndPopulateItems(nomination, request.getItems(), order);

        TruckNomination saved = truckNominationRepository.save(nomination);

        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_UPDATED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination Updated for order " + order.getOrderNumber() + " by " + username);

        // Compare old and new items for granular audit logs
        logNominationItemChanges(saved.getId(), order.getCustomer().getCustomerCode(), oldItems, saved.getItems());

        return truckNominationMapper.toResponse(saved);
    }

    private void logNominationItemChanges(Long nominationId, String customerCode, List<TruckNominationItem> oldItems, List<TruckNominationItem> newItems) {
        java.util.Map<String, TruckNominationItem> oldMap = oldItems.stream()
                .collect(Collectors.toMap(item -> item.getTruckNumber().trim().toUpperCase(), item -> item, (a, b) -> a));

        java.util.Map<String, TruckNominationItem> newMap = newItems.stream()
                .collect(Collectors.toMap(item -> item.getTruckNumber().trim().toUpperCase(), item -> item, (a, b) -> a));

        // Detect Added & Updated
        for (TruckNominationItem newItem : newItems) {
            String truckKey = newItem.getTruckNumber().trim().toUpperCase();
            if (!oldMap.containsKey(truckKey)) {
                auditLogService.log("TRUCK_ADDED", "TRUCK_NOMINATION", nominationId, customerCode,
                        "Truck " + newItem.getTruckNumber() + " added to nomination with quantity " + newItem.getAllocatedQuantity(),
                        null, newItem.getAllocatedQuantity().toString());
            } else {
                TruckNominationItem oldItem = oldMap.get(truckKey);
                if (oldItem.getAllocatedQuantity().compareTo(newItem.getAllocatedQuantity()) != 0) {
                    auditLogService.log("QUANTITY_UPDATED", "TRUCK_NOMINATION", nominationId, customerCode,
                            "Quantity updated for truck " + newItem.getTruckNumber() + " from " + oldItem.getAllocatedQuantity() + " to " + newItem.getAllocatedQuantity(),
                            oldItem.getAllocatedQuantity().toString(), newItem.getAllocatedQuantity().toString());
                }
            }
        }

        // Detect Removed
        for (TruckNominationItem oldItem : oldItems) {
            String truckKey = oldItem.getTruckNumber().trim().toUpperCase();
            if (!newMap.containsKey(truckKey)) {
                auditLogService.log("TRUCK_REMOVED", "TRUCK_NOMINATION", nominationId, customerCode,
                        "Truck " + oldItem.getTruckNumber() + " removed from nomination with quantity " + oldItem.getAllocatedQuantity(),
                        oldItem.getAllocatedQuantity().toString(), null);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TruckNominationResponse getNominationById(Long id) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));
        return truckNominationMapper.toResponse(nomination);
    }

    @Override
    @Transactional(readOnly = true)
    public TruckNominationResponse getNominationByOrderId(Long orderId) {
        TruckNomination nomination = truckNominationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found for order id: " + orderId));
        return truckNominationMapper.toResponse(nomination);
    }

    @Override
    public TruckNominationResponse submitNomination(Long id) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));

        if (!"DRAFT".equalsIgnoreCase(nomination.getStatus()) && !"REVISION_REQUESTED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Nomination must be in DRAFT or REVISION_REQUESTED status.");
        }

        if ("CUSTOMER_TRUCKS".equalsIgnoreCase(nomination.getTransportSource()) && (nomination.getItems() == null || nomination.getItems().isEmpty())) {
            throw new BadRequestException("At least one truck must be nominated.");
        }

        FuelOrder order = nomination.getOrder();
        BigDecimal approvedQuantity = order.getApprovedQuantity() != null ? order.getApprovedQuantity() : order.getQuantity();

        if ("CUSTOMER_TRUCKS".equalsIgnoreCase(nomination.getTransportSource())) {
            if (nomination.getNumberOfTrucks() == null || nomination.getNumberOfTrucks() <= 0) {
                throw new BadRequestException("Number of trucks must be specified and greater than 0 for Customer Transport.");
            }
            // Enforce exact equality for Customer Transport
            if (nomination.getTotalAllocatedQuantity().compareTo(approvedQuantity) != 0) {
                throw new BadRequestException("Total allocated quantity (" + nomination.getTotalAllocatedQuantity() + ") must exactly equal the approved customer order quantity (" + approvedQuantity + ") for Customer Transport.");
            }
        } else {
            // For Falcon Arranged, we allow submitting without truck assignments (0 allocation is fine)
            // But we must still ensure they don't exceed the approved quantity if any allocation was recorded
            if (nomination.getTotalAllocatedQuantity().compareTo(approvedQuantity) > 0) {
                throw new BadRequestException("Total allocated quantity (" + nomination.getTotalAllocatedQuantity() + ") cannot exceed approved customer order quantity (" + approvedQuantity + ").");
            }
        }

        String prevStatus = nomination.getStatus();
        nomination.setStatus("SUBMITTED");
        nomination.setRejectionReason(null);
        String prevOrderStatus = order.getOrderStatus();
        order.setOrderStatus("READY_FOR_LOADING");
        fuelOrderRepository.save(order);

        TruckNomination saved = truckNominationRepository.save(nomination);

        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_SUBMITTED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination Submitted for order " + order.getOrderNumber() + " by " + username, prevStatus, "SUBMITTED");
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevStatus + " to READY_FOR_LOADING for order " + order.getOrderNumber(), prevStatus, "READY_FOR_LOADING");

        return truckNominationMapper.toResponse(saved);
    }

    @Override
    public void requestChanges(Long id, String reason) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));

        if (!"SUBMITTED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Cannot request changes: Nomination status is " + nomination.getStatus() + ", but must be SUBMITTED.");
        }

        String prevStatus = nomination.getStatus();
        // Set nomination to REVISION_REQUESTED
        nomination.setStatus("REVISION_REQUESTED");
        nomination.setRejectionReason(reason);
        truckNominationRepository.save(nomination);

        // Set order status to REVISION_REQUESTED
        FuelOrder order = nomination.getOrder();
        String prevOrderStatus = order.getOrderStatus();
        order.setOrderStatus("REVISION_REQUESTED");
        fuelOrderRepository.save(order);

        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_REJECTED", "TRUCK_NOMINATION", nomination.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination status changed to REVISION_REQUESTED by " + username + ". Reason: " + reason, prevStatus, "REVISION_REQUESTED");
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevOrderStatus + " to REVISION_REQUESTED for order " + order.getOrderNumber(), prevOrderStatus, "REVISION_REQUESTED");
    }

    @Override
    public TruckNominationResponse approveNomination(Long id) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));

        if (!"SUBMITTED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Cannot approve nomination: status is " + nomination.getStatus() + ", but must be SUBMITTED.");
        }

        nomination.setStatus("APPROVED");
        TruckNomination saved = truckNominationRepository.save(nomination);

        FuelOrder order = nomination.getOrder();
        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_APPROVED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination Approved for order " + order.getOrderNumber() + " by " + username, "SUBMITTED", "APPROVED");

        return truckNominationMapper.toResponse(saved);
    }

    @Override
    public TruckNominationResponse cancelNomination(Long id) {
        TruckNomination nomination = truckNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck Nomination not found with id: " + id));

        if ("CANCELLED".equalsIgnoreCase(nomination.getStatus())) {
            throw new BadRequestException("Nomination is already cancelled.");
        }

        String prevStatus = nomination.getStatus();
        nomination.setStatus("CANCELLED");

        // Reset order status to PAYMENT_CONFIRMED
        FuelOrder order = nomination.getOrder();
        String prevOrderStatus = order.getOrderStatus();
        order.setOrderStatus("PAYMENT_CONFIRMED");
        fuelOrderRepository.save(order);

        TruckNomination saved = truckNominationRepository.save(nomination);

        String username = resolveCurrentUser();
        auditLogService.log("TRUCK_NOMINATION_CANCELLED", "TRUCK_NOMINATION", saved.getId(), order.getCustomer().getCustomerCode(),
                "Truck Nomination Cancelled for order " + order.getOrderNumber() + " by " + username, prevStatus, "CANCELLED");
        auditLogService.log("ORDER_STATUS_CHANGED", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(),
                "Order status changed from " + prevOrderStatus + " to PAYMENT_CONFIRMED for order " + order.getOrderNumber(), prevOrderStatus, "PAYMENT_CONFIRMED");

        return truckNominationMapper.toResponse(saved);
    }

    private void validateAndPopulateItems(TruckNomination nomination, List<TruckNominationItemDto> itemDtos, FuelOrder order) {
        if (itemDtos == null) {
            itemDtos = new ArrayList<>();
        }

        BigDecimal approvedQuantity = order.getApprovedQuantity() != null ? order.getApprovedQuantity() : order.getQuantity();
        BigDecimal totalAllocated = BigDecimal.ZERO;

        Set<String> truckNumbers = new HashSet<>();
        Set<String> trailerNumbers = new HashSet<>();

        List<TruckNominationItem> newItems = new ArrayList<>();

        for (TruckNominationItemDto dto : itemDtos) {
            // Validation 1: Negative quantities / capacities
            if (dto.getAllocatedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Allocated quantity cannot be negative.");
            }
            if (dto.getTruckCapacity().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Truck capacity cannot be negative.");
            }

            // Validation 2: Allocated quantity greater than truck capacity
            if (dto.getAllocatedQuantity().compareTo(dto.getTruckCapacity()) > 0) {
                throw new BadRequestException("Allocated quantity (" + dto.getAllocatedQuantity() + ") cannot be greater than truck capacity (" + dto.getTruckCapacity() + ").");
            }

            // Validation 3: Duplicate Truck Numbers
            String truckNumClean = dto.getTruckNumber().trim().toUpperCase();
            if (truckNumbers.contains(truckNumClean)) {
                throw new BadRequestException("Duplicate Truck Number found in nomination: " + dto.getTruckNumber());
            }
            truckNumbers.add(truckNumClean);

            // Validation 4: Duplicate Trailer Numbers
            String trailerNumClean = dto.getTrailerNumber().trim().toUpperCase();
            if (trailerNumbers.contains(trailerNumClean)) {
                throw new BadRequestException("Duplicate Trailer Number found in nomination: " + dto.getTrailerNumber());
            }
            trailerNumbers.add(trailerNumClean);

            totalAllocated = totalAllocated.add(dto.getAllocatedQuantity());

            TruckNominationItem item = TruckNominationItem.builder()
                    .nomination(nomination)
                    .truckNumber(dto.getTruckNumber())
                    .trailerNumber(dto.getTrailerNumber())
                    .driverName(dto.getDriverName())
                    .driverLicenceNumber(dto.getDriverLicenceNumber())
                    .driverPassport(dto.getDriverPassport())
                    .transportCompany(dto.getTransportCompany())
                    .destination(dto.getDestination())
                    .truckCapacity(dto.getTruckCapacity())
                    .allocatedQuantity(dto.getAllocatedQuantity())
                    .build();

            newItems.add(item);
        }

        // Validation 5: Total allocated quantity exceeding order quantity
        if (totalAllocated.compareTo(approvedQuantity) > 0) {
            throw new BadRequestException("Total allocated quantity (" + totalAllocated + ") cannot exceed approved customer order quantity (" + approvedQuantity + ").");
        }

        // Update items list
        if (nomination.getItems() == null) {
            nomination.setItems(new ArrayList<>());
        } else {
            nomination.getItems().clear();
        }
        nomination.getItems().addAll(newItems);
        nomination.setTotalAllocatedQuantity(totalAllocated);
    }

    private String resolveCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
