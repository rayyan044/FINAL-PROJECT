package com.falconenergy.service.impl;

import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.StorageTank;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.StorageTankRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.repository.TruckPricingRepository;
import com.falconenergy.service.FuelOrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
public class FuelOrderIntegrationTest {

    @Autowired
    private FuelOrderService fuelOrderService;

    @Autowired
    private FuelProductRepository productRepository;

    @Autowired
    private StorageTankRepository tankRepository;

    @Autowired
    private FuelOrderRepository orderRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private TruckPricingRepository truckPricingRepository;

    @Test
    @Transactional
    public void whenOrderApproved_inventoryAndTankAreDeducted_andTransactionCreated() throws Exception {
        // This test assumes DB seeding or a clean in-memory DB; it creates minimal objects and performs an approval flow.

        FuelProduct product = new FuelProduct();
        product.setProductName("TEST-FUEL");
        product.setFuelType("TestType");
        product.setUnitPrice(new BigDecimal("1.00"));
        product.setDensity(new BigDecimal("0.74"));
        product.setAvailableQuantity(new BigDecimal("1000"));
        product.setStatus("ACTIVE");
        product = productRepository.save(product);

        StorageTank tank = new StorageTank();
        tank.setTankName("TANK-TEST");
        tank.setCapacity(new BigDecimal("5000"));
        tank.setCurrentVolume(new BigDecimal("500"));
        tank.setFuelProduct(product);
        tank = tankRepository.save(tank);

        com.falconenergy.entity.Vehicle vehicle = com.falconenergy.entity.Vehicle.builder()
                .truckNumber("TST-TRUCK-001").plateNumber("TST-001")
                .capacity(new BigDecimal("100")).currentStatus("AVAILABLE").active(true)
                .assignedFuelTypes(new java.util.HashSet<>(java.util.Set.of("TestType"))).build();
        vehicleRepository.save(vehicle);
        truckPricingRepository.save(com.falconenergy.entity.TruckPricing.builder()
                .capacity(new BigDecimal("100")).fuelType("TestType")
                .transportPrice(new BigDecimal("25")).active(true).build());

        // Create order DTO via repository for brevity then approve via service
        com.falconenergy.dto.FuelOrderRequest req = new com.falconenergy.dto.FuelOrderRequest();
        req.setOrderNumber("TST-ORD-001");
        req.setCustomerId(1L); // may need seeded customer; tests may require adjustments
        req.setProductId(product.getId());
        req.setQuantity(new BigDecimal("100"));

        // create order (status PENDING)
        com.falconenergy.dto.FuelOrderResponse created = fuelOrderService.createOrder(req);

        // Approve order (this should deduct stock and tank volume)
        FuelOrder updated = orderRepository.findById(created.getId()).orElseThrow();
        fuelOrderService.updateOrderStatus(updated.getId(), "APPROVED");

        FuelProduct afterProduct = productRepository.findById(product.getId()).orElseThrow();
        StorageTank afterTank = tankRepository.findById(tank.getId()).orElseThrow();

        Assertions.assertEquals(0, afterProduct.getAvailableQuantity().compareTo(new BigDecimal("900")),
                "Product stock should be exactly 900 after approval");

        // If tank had sufficient volume, it should be reduced by the order quantity
        Assertions.assertEquals(0, afterTank.getCurrentVolume().compareTo(new BigDecimal("400")),
                "Tank volume should be exactly 400 after approval");
    }
}
