package com.falconenergy.repository;
import com.falconenergy.entity.TransportReleaseForm;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface TransportReleaseFormRepository extends JpaRepository<TransportReleaseForm, Long> {
    Optional<TransportReleaseForm> findByLoadingActivityId(Long activityId);
    @Query("SELECT MAX(t.releaseFormNumber) FROM TransportReleaseForm t WHERE t.releaseFormNumber LIKE :prefix%") String findMaxReleaseFormNumberWithPrefix(@Param("prefix") String prefix);
}
