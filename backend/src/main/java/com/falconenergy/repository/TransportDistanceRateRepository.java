package com.falconenergy.repository;

import com.falconenergy.entity.TransportDistanceRate;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TransportDistanceRateRepository extends JpaRepository<TransportDistanceRate, Long> {
    @Query("select r from TransportDistanceRate r where r.active=true and :km >= r.minimumKm and (r.maximumKm is null or :km <= r.maximumKm)")
    List<TransportDistanceRate> matches(@Param("km") BigDecimal km);
    @Query("select count(r)>0 from TransportDistanceRate r where r.active=true and r.id<>coalesce(:excludeId,-1) and (:maximumKm is null or r.minimumKm<=:maximumKm) and (r.maximumKm is null or r.maximumKm>=:minimumKm)")
    boolean overlapsActive(@Param("minimumKm") BigDecimal minimumKm, @Param("maximumKm") BigDecimal maximumKm, @Param("excludeId") Long excludeId);
}
