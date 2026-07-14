package com.falconenergy.repository;

import com.falconenergy.entity.TruckNomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TruckNominationRepository extends JpaRepository<TruckNomination, Long> {
    Optional<TruckNomination> findByOrderId(Long orderId);
}
