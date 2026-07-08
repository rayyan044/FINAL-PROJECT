package com.falconenergy.service.impl;

import com.falconenergy.dto.FuelTransactionRequest;
import com.falconenergy.dto.FuelTransactionResponse;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.FuelTransaction;
import com.falconenergy.entity.Inventory;
import com.falconenergy.entity.StorageTank;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.FuelTransactionMapper;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.FuelTransactionRepository;
import com.falconenergy.repository.InventoryRepository;
import com.falconenergy.repository.StorageTankRepository;
import com.falconenergy.service.FuelTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class FuelTransactionServiceImpl implements FuelTransactionService {

    private final FuelTransactionRepository fuelTransactionRepository;
    private final FuelProductRepository fuelProductRepository;
    private final FuelTransactionMapper fuelTransactionMapper;
    private final InventoryRepository inventoryRepository;
    private final StorageTankRepository storageTankRepository;

    public FuelTransactionServiceImpl(
            FuelTransactionRepository fuelTransactionRepository,
            FuelProductRepository fuelProductRepository,
            FuelTransactionMapper fuelTransactionMapper,
            InventoryRepository inventoryRepository,
            StorageTankRepository storageTankRepository
    ) {
        this.fuelTransactionRepository = fuelTransactionRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.fuelTransactionMapper = fuelTransactionMapper;
        this.inventoryRepository = inventoryRepository;
        this.storageTankRepository = storageTankRepository;
    }

    @Override
    public FuelTransactionResponse createTransaction(FuelTransactionRequest request) {
        log.info("Processing transaction: {}", request.getTransactionNumber());
        if (fuelTransactionRepository.existsByTransactionNumber(request.getTransactionNumber())) {
            throw new DuplicateResourceException("Transaction number already exists: " + request.getTransactionNumber());
        }

        FuelProduct product = fuelProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel product not found with id: " + request.getProductId()));

        BigDecimal qty = request.getQuantity();
        String type = request.getTransactionType(); // Purchase, Sale, Transfer, Adjustment

        // Validate and Adjust product stock levels
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal issued = BigDecimal.ZERO;

        if ("Purchase".equalsIgnoreCase(type)) {
            product.setAvailableQuantity(product.getAvailableQuantity().add(qty));
            received = qty;
        } else if ("Sale".equalsIgnoreCase(type) || "Transfer".equalsIgnoreCase(type)) {
            if (product.getAvailableQuantity().compareTo(qty) < 0) {
                throw new BadRequestException("Insufficient inventory available. Remaining: " + product.getAvailableQuantity());
            }
            product.setAvailableQuantity(product.getAvailableQuantity().subtract(qty));
            issued = qty;
        } else if ("Adjustment".equalsIgnoreCase(type)) {
            // Can be positive or negative
            BigDecimal targetQty = product.getAvailableQuantity().add(qty);
            if (targetQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Adjustment results in negative stock: " + targetQty);
            }
            product.setAvailableQuantity(targetQty);
            if (qty.compareTo(BigDecimal.ZERO) >= 0) {
                received = qty;
            } else {
                issued = qty.negate();
            }
        } else {
            throw new BadRequestException("Invalid transaction type: " + type);
        }

        fuelProductRepository.save(product);

        // Record Fuel Transaction
        FuelTransaction transaction = fuelTransactionMapper.toEntity(request);
        transaction.setProduct(product);
        if (request.getTankId() != null) {
            StorageTank tank = storageTankRepository.findById(request.getTankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Storage tank not found with id: " + request.getTankId()));
            transaction.setStorageTank(tank);
        }
        FuelTransaction savedTxn = fuelTransactionRepository.save(transaction);

        // Update Daily Inventory Sheet
        LocalDate today = LocalDate.now();
        Inventory inventory = inventoryRepository.findByProductIdAndRecordDate(product.getId(), today)
                .orElseGet(() -> {
                    // Fetch yesterday's closing stock to set as today's opening stock
                    BigDecimal opening = inventoryRepository.findByProductIdAndRecordDate(product.getId(), today.minusDays(1))
                            .map(Inventory::getClosingStock)
                            .orElse(product.getAvailableQuantity().subtract(qty)); // Fallback estimate

                    return Inventory.builder()
                            .product(product)
                            .openingStock(opening)
                            .receivedStock(BigDecimal.ZERO)
                            .issuedStock(BigDecimal.ZERO)
                            .closingStock(opening)
                            .recordDate(today)
                            .build();
                });

        inventory.setReceivedStock(inventory.getReceivedStock().add(received));
        inventory.setIssuedStock(inventory.getIssuedStock().add(issued));
        // closingStock = openingStock + receivedStock - issuedStock
        inventory.setClosingStock(inventory.getOpeningStock().add(inventory.getReceivedStock()).subtract(inventory.getIssuedStock()));

        inventoryRepository.save(inventory);

        log.info("Transaction processed successfully and daily stock inventory updated");
        return fuelTransactionMapper.toResponse(savedTxn);
    }

    @Override
    @Transactional(readOnly = true)
    public FuelTransactionResponse getTransactionById(Long id) {
        FuelTransaction transaction = fuelTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel transaction not found with id: " + id));
        return fuelTransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FuelTransactionResponse> getAllTransactions(
            String search,
            String type,
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Specification<FuelTransaction> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("transactionNumber")), wildcard));
        }

        if (type != null && !type.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("transactionType")), type.toLowerCase()));
        }

        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
        }

        return fuelTransactionRepository.findAll(spec, pageable).map(fuelTransactionMapper::toResponse);
    }
}
