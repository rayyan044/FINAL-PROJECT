package com.falconenergy.repository;

import com.falconenergy.entity.FuelProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelProductRepository extends JpaRepository<FuelProduct, Long>, JpaSpecificationExecutor<FuelProduct> {
    Optional<FuelProduct> findByProductName(String productName);
    boolean existsByProductName(String productName);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXISTS(SELECT 1 FROM fuel_products WHERE LOWER(product_name) = LOWER(:productName) AND deleted = false)", nativeQuery = true)
    boolean existsByProductNameIgnoreCaseUnfiltered(@org.springframework.data.repository.query.Param("productName") String productName);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXISTS(SELECT 1 FROM fuel_products WHERE LOWER(product_name) = LOWER(:productName) AND id <> :excludeId AND deleted = false)", nativeQuery = true)
    boolean existsByProductNameIgnoreCaseUnfilteredExcludeId(@org.springframework.data.repository.query.Param("productName") String productName, @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM FuelProduct p WHERE p.id = :id")
    Optional<FuelProduct> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM fuel_products WHERE id = :id", nativeQuery = true)
    Optional<FuelProduct> findByIdIncludingDeleted(@org.springframework.data.repository.query.Param("id") Long id);
}
