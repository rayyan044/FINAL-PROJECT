package com.falconenergy.service.impl;
import java.util.Optional;
import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.FuelTransaction;
import com.falconenergy.entity.StorageTank;
import com.falconenergy.dto.FuelTransactionRequest;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.FuelOrderMapper;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.FuelTransactionRepository;
import com.falconenergy.service.FuelOrderService;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.StorageTankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.entity.Invoice;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.dto.FuelOrderEditApprovalRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class FuelOrderServiceImpl implements FuelOrderService {

    private final FuelOrderRepository fuelOrderRepository;
    private final CustomerRepository customerRepository;
    private final FuelProductRepository fuelProductRepository;
    private final FuelOrderMapper fuelOrderMapper;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final StorageTankService storageTankService;
    private final com.falconenergy.repository.StorageTankRepository storageTankRepository;
    private final AuditLogService auditLogService;
    private final com.falconenergy.service.FuelTransactionService fuelTransactionService;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public FuelOrderServiceImpl(
            FuelOrderRepository fuelOrderRepository,
            CustomerRepository customerRepository,
            FuelProductRepository fuelProductRepository,
            FuelOrderMapper fuelOrderMapper,
            FuelTransactionRepository fuelTransactionRepository,
            StorageTankService storageTankService,
            com.falconenergy.repository.StorageTankRepository storageTankRepository,
            AuditLogService auditLogService,
            com.falconenergy.service.FuelTransactionService fuelTransactionService,
            UserRepository userRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.fuelOrderRepository = fuelOrderRepository;
        this.customerRepository = customerRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.fuelOrderMapper = fuelOrderMapper;
        this.fuelTransactionRepository = fuelTransactionRepository;
        this.storageTankService = storageTankService;
        this.storageTankRepository = storageTankRepository;
        this.auditLogService = auditLogService;
        this.fuelTransactionService = fuelTransactionService;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public FuelOrderResponse createOrder(FuelOrderRequest request) {
        log.info("Creating fuel order: {}", request.getOrderNumber());
        if (fuelOrderRepository.existsByOrderNumber(request.getOrderNumber())) {
            throw new DuplicateResourceException("Order number already exists: " + request.getOrderNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        FuelProduct product = fuelProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getProductId()));

        // Calculate order amount: quantity * unit price
        BigDecimal amount = request.getQuantity().multiply(product.getUnitPrice());

        FuelOrder order = fuelOrderMapper.toEntity(request);
        order.setCustomer(customer);
        order.setProduct(product);
        order.setAmount(amount);
        order.setOrderStatus("PENDING");

        FuelOrder savedOrder = fuelOrderRepository.save(order);

        log.info("Fuel order created successfully with status PENDING (stock deduction deferred to confirmation)");
        FuelOrderResponse response = fuelOrderMapper.toResponse(savedOrder);
        nullifyProductQuantityIfCustomer(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FuelOrderResponse getOrderById(Long id) {
        FuelOrder order = fuelOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String principal = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByEmail(principal)
                    .or(() -> userRepository.findByUsername(principal));
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                if (currentUser.getRole() == UserRole.CUSTOMER) {
                    boolean isOwner = (order.getCustomer() != null && (
                        (order.getCustomer().getEmail() != null && order.getCustomer().getEmail().equalsIgnoreCase(currentUser.getEmail())) ||
                        (order.getCustomer().getCustomerCode() != null && order.getCustomer().getCustomerCode().equalsIgnoreCase(currentUser.getUsername()))
                    ));
                    if (!isOwner) {
                        throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only view your own fuel requests.");
                    }
                }
            }
        }

        FuelOrderResponse response = fuelOrderMapper.toResponse(order);
        nullifyProductQuantityIfCustomer(response);
        return response;
    }

    @Override
    public FuelOrderResponse updateOrder(Long id, FuelOrderRequest request) {
        log.info("Updating fuel order with id: {}", id);
        FuelOrder order = fuelOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + id));

        if (!order.getOrderNumber().equals(request.getOrderNumber()) &&
                fuelOrderRepository.existsByOrderNumber(request.getOrderNumber())) {
            throw new DuplicateResourceException("Order number already exists: " + request.getOrderNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        FuelProduct product = fuelProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getProductId()));

        // Check if the order was already APPROVED
        boolean wasApproved = "APPROVED".equalsIgnoreCase(order.getOrderStatus());
        
        if (wasApproved) {
            // Return old quantity to stock first before checking new capacity
            BigDecimal restoredStock = order.getProduct().getAvailableQuantity().add(order.getQuantity());
            
            if (product.getId().equals(order.getProduct().getId())) {
                if (restoredStock.compareTo(request.getQuantity()) < 0) {
                    throw new BadRequestException("Insufficient fuel product stock. Available: " 
                            + restoredStock + ", Requested: " + request.getQuantity());
                }
                product.setAvailableQuantity(restoredStock.subtract(request.getQuantity()));
                if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    product.setStatus("UNAVAILABLE");
                } else {
                    product.setStatus("ACTIVE");
                }
                fuelProductRepository.save(product);
            } else {
                // Different product
                if (product.getAvailableQuantity().compareTo(request.getQuantity()) < 0) {
                    throw new BadRequestException("Insufficient fuel product stock. Available: " 
                            + product.getAvailableQuantity() + ", Requested: " + request.getQuantity());
                }
                // Restore original product
                FuelOrder originalOrder = fuelOrderRepository.findById(id).get();
                FuelProduct origProduct = originalOrder.getProduct();
                origProduct.setAvailableQuantity(origProduct.getAvailableQuantity().add(originalOrder.getQuantity()));
                if (origProduct.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0 && "UNAVAILABLE".equals(origProduct.getStatus())) {
                    origProduct.setStatus("ACTIVE");
                }
                fuelProductRepository.save(origProduct);

                // Deduct from new product
                product.setAvailableQuantity(product.getAvailableQuantity().subtract(request.getQuantity()));
                if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    product.setStatus("UNAVAILABLE");
                } else {
                    product.setStatus("ACTIVE");
                }
                fuelProductRepository.save(product);
            }
        }

        BigDecimal amount = request.getQuantity().multiply(product.getUnitPrice());

        fuelOrderMapper.updateEntityFromRequest(request, order);
        order.setCustomer(customer);
        order.setProduct(product);
        order.setAmount(amount);

        FuelOrder updated = fuelOrderRepository.save(order);
        log.info("Fuel order updated successfully with amount: {}", amount);
        FuelOrderResponse response = fuelOrderMapper.toResponse(updated);
        nullifyProductQuantityIfCustomer(response);
        return response;
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String principalName = authentication.getName();
        return principalName != null && !principalName.isBlank() ? principalName : "system";
    }

    private void nullifyProductQuantityIfCustomer(FuelOrderResponse response) {
        if (response != null && response.getProduct() != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String principal = authentication.getName();
                Optional<User> currentUserOpt = userRepository.findByEmail(principal)
                        .or(() -> userRepository.findByUsername(principal));
                if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == UserRole.CUSTOMER) {
                    response.getProduct().setAvailableQuantity(null);
                }
            }
        }
    }

    @Override
    public FuelOrderResponse updateOrderStatus(Long id, String status) {
        log.info("Updating status for fuel order {} to {}", id, status);
        FuelOrder order = fuelOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + id));

        String newStatus = status.toUpperCase();
        String oldStatus = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";

        if (newStatus.equals("APPROVED") && !oldStatus.equals("APPROVED")) {
            // Approve / Confirm logic: Verify stock and deduct
            FuelProduct product = order.getProduct();
            BigDecimal requestedQty = order.getQuantity();

            java.util.List<StorageTank> tanks = storageTankRepository.findByFuelProductId(product.getId());
            BigDecimal totalTankVolume = tanks.stream()
                    .map(StorageTank::getCurrentVolume)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean hasTanks = !tanks.isEmpty();

            if (product.getAvailableQuantity().compareTo(requestedQty) < 0 || (hasTanks && totalTankVolume.compareTo(requestedQty) < 0)) {
                // Do not deduct stock, set status to AWAITING_RESTOCK
                order.setOrderStatus("AWAITING_RESTOCK");
                FuelOrder savedOrder = fuelOrderRepository.save(order);
                
                String actor = resolveCurrentUser();
                auditLogService.log(
                        "ORDER_AWAITING_RESTOCK",
                        "FUEL_ORDER",
                        savedOrder.getId(),
                        savedOrder.getOrderNumber(),
                        "Order approval set to AWAITING_RESTOCK due to insufficient product or tank stock. Processed by: " + actor
                );
                return fuelOrderMapper.toResponse(savedOrder);
            }

            BigDecimal qtyBefore = product.getAvailableQuantity();

            // Perform deductions
            if (hasTanks) {
                BigDecimal remainingToDeduct = requestedQty;
                for (StorageTank tank : tanks) {
                    if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal tankVol = tank.getCurrentVolume();
                    if (tankVol != null && tankVol.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal toDeduct = tankVol.min(remainingToDeduct);
                        
                        // Deduct from storage tank volume
                        storageTankService.adjustVolume(tank.getId(), toDeduct.negate(), false);
                        
                        // Create transaction via FuelTransactionService
                        com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                                .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .productId(product.getId())
                                .quantity(toDeduct)
                                .transactionType("Sale")
                                .transactionDate(LocalDateTime.now())
                                .tankId(tank.getId())
                                .build();
                        fuelTransactionService.createTransaction(txnReq);
                        
                        remainingToDeduct = remainingToDeduct.subtract(toDeduct);
                    }
                }
            } else {
                // Deduct only from product stock
                com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                        .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .productId(product.getId())
                        .quantity(requestedQty)
                        .transactionType("Sale")
                        .transactionDate(LocalDateTime.now())
                        .build();
                fuelTransactionService.createTransaction(txnReq);
            }

            // Reload product to get new availableQuantity and update availability status if needed
            product = fuelProductRepository.findById(product.getId()).orElse(product);
            if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) == 0) {
                product.setStatus("UNAVAILABLE");
                fuelProductRepository.save(product);
            }

            BigDecimal remainingQty = product.getAvailableQuantity();
            order.setAmount(requestedQty.multiply(product.getUnitPrice()));

            // Log detailed audit trail
            String actor = resolveCurrentUser();
            String auditDetails = String.format(
                "Fuel Type: %s, Quantity Before Update: %s, Quantity Deducted: %s, Remaining Quantity: %s, Order ID: %d, Customer ID: %d, Confirmed By: %s, Date and Time: %s",
                product.getFuelType(),
                qtyBefore.toString(),
                requestedQty.toString(),
                remainingQty.toString(),
                order.getId(),
                order.getCustomer().getId(),
                actor,
                LocalDateTime.now().toString()
            );
            auditLogService.log("INVENTORY_DEDUCTION", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(), auditDetails);

        } else if (!newStatus.equals("APPROVED") && oldStatus.equals("APPROVED")) {
            // Cancellation / Rejection after approval: Restore stock
            FuelProduct product = order.getProduct();
            java.util.List<StorageTank> tanks = storageTankRepository.findByFuelProductId(product.getId());

            BigDecimal remainingToRestore = order.getQuantity();

            if (!tanks.isEmpty()) {
                for (StorageTank tank : tanks) {
                    if (remainingToRestore.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal capacity = tank.getCapacity();
                    BigDecimal current = tank.getCurrentVolume() != null ? tank.getCurrentVolume() : BigDecimal.ZERO;
                    BigDecimal room = capacity.subtract(current);
                    if (room.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal toRestore = room.min(remainingToRestore);
                        
                        // Restore tank volume
                        storageTankService.adjustVolume(tank.getId(), toRestore);
                        
                        // Create adjustment transaction via FuelTransactionService
                        com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                                .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .productId(product.getId())
                                .quantity(toRestore)
                                .transactionType("Adjustment")
                                .transactionDate(LocalDateTime.now())
                                .tankId(tank.getId())
                                .build();
                        fuelTransactionService.createTransaction(txnReq);
                        
                        remainingToRestore = remainingToRestore.subtract(toRestore);
                    }
                }
            }

            // Create adjustment transaction for any remainder
            if (remainingToRestore.compareTo(BigDecimal.ZERO) > 0) {
                com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                        .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .productId(product.getId())
                        .quantity(remainingToRestore)
                        .transactionType("Adjustment")
                        .transactionDate(LocalDateTime.now())
                        .build();
                fuelTransactionService.createTransaction(txnReq);
            }

            // Update availability status if stock becomes positive
            product = fuelProductRepository.findById(product.getId()).orElse(product);
            if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0 && "UNAVAILABLE".equals(product.getStatus())) {
                product.setStatus("ACTIVE");
                fuelProductRepository.save(product);
            }

            // Log detailed audit trail
            String actor = resolveCurrentUser();
            auditLogService.log("INVENTORY_RESTORE", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(), 
                "Restored stock from order status update to " + newStatus + ". Restored Quantity: " + order.getQuantity() + ". Restored By: " + actor);
        }

        order.setOrderStatus(newStatus);
        FuelOrder updated = fuelOrderRepository.save(order);
        if ("APPROVED".equalsIgnoreCase(newStatus) && !"APPROVED".equalsIgnoreCase(oldStatus)) {
            generateInvoiceForOrder(updated);
        }
        FuelOrderResponse response = fuelOrderMapper.toResponse(updated);
        nullifyProductQuantityIfCustomer(response);
        return response;
    }

    @Override
    public void deleteOrder(Long id) {
        log.info("Deleting fuel order with id: {}", id);
        FuelOrder order = fuelOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + id));
        
        // Restore stock ONLY if it was APPROVED
        if ("APPROVED".equalsIgnoreCase(order.getOrderStatus())) {
            FuelProduct product = order.getProduct();
            product.setAvailableQuantity(product.getAvailableQuantity().add(order.getQuantity()));
            if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0 && "UNAVAILABLE".equals(product.getStatus())) {
                product.setStatus("ACTIVE");
            }
            fuelProductRepository.save(product);

            // Cancel sale transaction log (log reversal adjustment)
            FuelTransaction txn = FuelTransaction.builder()
                    .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .product(product)
                    .quantity(order.getQuantity())
                    .transactionType("Adjustment")
                    .transactionDate(LocalDateTime.now())
                    .build();
            fuelTransactionRepository.save(txn);
        }

        fuelOrderRepository.delete(order);
        log.info("Fuel order deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FuelOrderResponse> getAllOrders(
            String search,
            String status,
            Long customerId,
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Specification<FuelOrder> spec = Specification.where(null);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String principal = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByEmail(principal)
                    .or(() -> userRepository.findByUsername(principal));
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                if (currentUser.getRole() == UserRole.CUSTOMER) {
                    spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(cb.lower(root.get("customer").get("email")), currentUser.getEmail().toLowerCase()),
                        cb.equal(cb.lower(root.get("customer").get("customerCode")), currentUser.getUsername().toLowerCase())
                    ));
                }
            }
        }

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("orderNumber")), wildcard));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("orderStatus")), status.toLowerCase()));
        }

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }

        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("orderDate"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("orderDate"), endDate));
        }

        Page<FuelOrderResponse> responses = fuelOrderRepository.findAll(spec, pageable).map(fuelOrderMapper::toResponse);
        responses.forEach(this::nullifyProductQuantityIfCustomer);
        return responses;
    }

    @Override
    public FuelOrderResponse approveOrderWithEdit(Long id, FuelOrderEditApprovalRequest request) {
        log.info("Approving fuel order {} with edited quantity {}", id, request.getApprovedQuantity());
        FuelOrder order = fuelOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + id));

        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus()) && !"AWAITING_RESTOCK".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Order is not in a status that can be approved. Current status: " + order.getOrderStatus());
        }

        BigDecimal approvedQty = request.getApprovedQuantity();
        FuelProduct product = order.getProduct();

        // 1. Validate edited quantity against current available stock
        java.util.List<StorageTank> tanks = storageTankRepository.findByFuelProductId(product.getId());
        BigDecimal totalTankVolume = tanks.stream()
                .map(StorageTank::getCurrentVolume)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasTanks = !tanks.isEmpty();

        if (product.getAvailableQuantity().compareTo(approvedQty) < 0 || (hasTanks && totalTankVolume.compareTo(approvedQty) < 0)) {
            throw new BadRequestException("Approved quantity (" + approvedQty + " L) exceeds available stock (" + product.getAvailableQuantity() + " L) or tank capacity.");
        }

        // 2. Record history on the order
        order.setOriginalQuantity(order.getQuantity());
        order.setApprovedQuantity(approvedQty);
        order.setEditReason(request.getReason());

        // Update the order quantity and amount
        order.setQuantity(approvedQty);
        BigDecimal newAmount = approvedQty.multiply(product.getUnitPrice());
        order.setAmount(newAmount);

        BigDecimal qtyBefore = product.getAvailableQuantity();

        // 3. Deduct approved quantity from inventory
        if (hasTanks) {
            BigDecimal remainingToDeduct = approvedQty;
            for (StorageTank tank : tanks) {
                if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal tankVol = tank.getCurrentVolume();
                if (tankVol != null && tankVol.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal toDeduct = tankVol.min(remainingToDeduct);
                    storageTankService.adjustVolume(tank.getId(), toDeduct.negate(), false);

                    com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                            .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .productId(product.getId())
                            .quantity(toDeduct)
                            .transactionType("Sale")
                            .transactionDate(LocalDateTime.now())
                            .tankId(tank.getId())
                            .build();
                    fuelTransactionService.createTransaction(txnReq);

                    remainingToDeduct = remainingToDeduct.subtract(toDeduct);
                }
            }
        } else {
            com.falconenergy.dto.FuelTransactionRequest txnReq = com.falconenergy.dto.FuelTransactionRequest.builder()
                    .transactionNumber("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .productId(product.getId())
                    .quantity(approvedQty)
                    .transactionType("Sale")
                    .transactionDate(LocalDateTime.now())
                    .build();
            fuelTransactionService.createTransaction(txnReq);
        }

        // Reload product & update status if unavailable
        product = fuelProductRepository.findById(product.getId()).orElse(product);
        if (product.getAvailableQuantity().compareTo(BigDecimal.ZERO) == 0) {
            product.setStatus("UNAVAILABLE");
            fuelProductRepository.save(product);
        }

        // 4. Update order status to APPROVED
        order.setOrderStatus("APPROVED");
        FuelOrder savedOrder = fuelOrderRepository.save(order);

        // 5. Audit logs
        String actor = resolveCurrentUser();
        String auditDetails = String.format(
            "Fuel Type: %s, Quantity Before Update: %s, Quantity Deducted: %s, Remaining Quantity: %s, Order ID: %d, Customer ID: %d, Confirmed By: %s, Date and Time: %s",
            product.getFuelType(),
            qtyBefore.toString(),
            approvedQty.toString(),
            product.getAvailableQuantity().toString(),
            order.getId(),
            order.getCustomer().getId(),
            actor,
            LocalDateTime.now().toString()
        );
        auditLogService.log("INVENTORY_DEDUCTION", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(), auditDetails);

        String editHistoryDetails = String.format(
            "Order approved with edited quantity. Original requested quantity: %s, Approved quantity: %s, Reason: %s, Processed by: %s",
            order.getOriginalQuantity(),
            order.getApprovedQuantity(),
            order.getEditReason() != null ? order.getEditReason() : "None",
            actor
        );
        auditLogService.log("ORDER_APPROVED_WITH_EDIT", "FUEL_ORDER", order.getId(), order.getCustomer().getCustomerCode(), editHistoryDetails);

        // 6. Generate Invoice
        generateInvoiceForOrder(savedOrder);

        FuelOrderResponse response = fuelOrderMapper.toResponse(savedOrder);
        nullifyProductQuantityIfCustomer(response);
        return response;
    }

    private void generateInvoiceForOrder(FuelOrder order) {
        log.info("Generating invoice for order {}", order.getOrderNumber());

        if (invoiceRepository.existsByOrderId(order.getId())) {
            log.warn("Invoice already exists for order id: {}", order.getId());
            return;
        }

        BigDecimal subtotal = order.getAmount();
        BigDecimal taxRate = new BigDecimal("0.16"); // 16% VAT
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(tax).setScale(2, java.math.RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + order.getOrderNumber())
                .invoiceDate(LocalDateTime.now())
                .order(order)
                .subtotal(subtotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .paymentStatus("PENDING_PAYMENT")
                .termsAndConditions("1. Payment is due within 14 days of invoice date.\n2. Interest will be charged at 1.5% per month on late payments.\n3. Fuel deliveries are subject to standard terms of carriage.")
                .build();

        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setCreatedBy(resolveCurrentUser());
        invoice.setUpdatedBy(resolveCurrentUser());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        if (savedInvoice == null) {
            savedInvoice = invoice;
        }
        log.info("Invoice generated successfully with invoice number: {}", savedInvoice.getInvoiceNumber());

        auditLogService.log(
                "INVOICE_GENERATED",
                "INVOICE",
                savedInvoice.getId() != null ? savedInvoice.getId() : 0L,
                order.getCustomer().getCustomerCode(),
                "Invoice " + savedInvoice.getInvoiceNumber() + " automatically generated for order " + order.getOrderNumber()
        );
    }
}
