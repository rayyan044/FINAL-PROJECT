package com.falconenergy.repository;

import com.falconenergy.entity.StorageTank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface StorageTankRepository extends JpaRepository<StorageTank, Long>, JpaSpecificationExecutor<StorageTank> {
    Optional<StorageTank> findByTankName(String tankName);
    boolean existsByTankName(String tankName);

    Optional<StorageTank> findFirstByFuelProductIdAndCurrentVolumeGreaterThanEqual(Long productId, BigDecimal qty);
    java.util.List<StorageTank> findByFuelProductId(Long productId);
}
