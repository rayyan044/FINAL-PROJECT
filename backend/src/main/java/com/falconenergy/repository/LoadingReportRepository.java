package com.falconenergy.repository;

import com.falconenergy.entity.LoadingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoadingReportRepository extends JpaRepository<LoadingReport, Long> {

    @Query("SELECT MAX(lr.reportNumber) FROM LoadingReport lr WHERE lr.reportNumber LIKE :prefix%")
    String findMaxReportNumberWithPrefix(@Param("prefix") String prefix);

    /**
     * Legacy data can contain more than one report for an activity.  Consumers
     * need the report that was generated most recently rather than a query that
     * fails with NonUniqueResultException.
     */
    Optional<LoadingReport> findFirstByLoadingActivityIdOrderByCreatedAtDesc(Long loadingActivityId);

    List<LoadingReport> findByLoadingOrderId(Long loadingOrderId);
}
