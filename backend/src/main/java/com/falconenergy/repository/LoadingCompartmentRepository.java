package com.falconenergy.repository;

import com.falconenergy.entity.LoadingCompartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadingCompartmentRepository extends JpaRepository<LoadingCompartment, Long> {
}
