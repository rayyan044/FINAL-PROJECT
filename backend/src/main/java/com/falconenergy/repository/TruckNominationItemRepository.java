package com.falconenergy.repository;

import com.falconenergy.entity.TruckNominationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TruckNominationItemRepository extends JpaRepository<TruckNominationItem, Long> {
}
