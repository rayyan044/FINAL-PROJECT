package com.falconenergy.repository;

import com.falconenergy.entity.FuelPriceRange;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface FuelPriceRangeRepository extends JpaRepository<FuelPriceRange, Long> {
    @Query("""
        select r from FuelPriceRange r
        where r.fuelProduct.id = :productId and r.status = 'ACTIVE' and r.effectiveDate <= :orderDate
          and :quantity between r.minLitres and r.maxLitres
        order by r.effectiveDate desc
        """)
    List<FuelPriceRange> findActiveMatches(@Param("productId") Long productId, @Param("quantity") BigDecimal quantity,
                                           @Param("orderDate") LocalDate orderDate);

    @Query("""
        select count(r) > 0 from FuelPriceRange r
        where r.fuelProduct.id = :productId and r.id <> coalesce(:excludeId, -1)
          and r.minLitres <= :maxLitres and r.maxLitres >= :minLitres
        """)
    boolean existsOverlappingRange(@Param("productId") Long productId, @Param("minLitres") BigDecimal minLitres,
                                   @Param("maxLitres") BigDecimal maxLitres, @Param("excludeId") Long excludeId);
}
