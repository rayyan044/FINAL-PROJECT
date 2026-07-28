package com.falconenergy.repository;

import com.falconenergy.entity.ReportSnapshot;
import com.falconenergy.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, Long> {
    Optional<ReportSnapshot> findByReportNumber(String reportNumber);
    List<ReportSnapshot> findByReportType(ReportType reportType);
    List<ReportSnapshot> findByGeneratedBy(String username);
}
