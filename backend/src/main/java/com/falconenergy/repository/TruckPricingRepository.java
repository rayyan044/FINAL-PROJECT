package com.falconenergy.repository;
import com.falconenergy.entity.TruckPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
public interface TruckPricingRepository extends JpaRepository<TruckPricing, Long> {
    Optional<TruckPricing> findByCapacityAndFuelTypeIgnoreCaseAndActiveTrue(BigDecimal capacity, String fuelType);
    Optional<TruckPricing> findByCapacityAndFuelTypeIgnoreCase(BigDecimal capacity, String fuelType);
}
