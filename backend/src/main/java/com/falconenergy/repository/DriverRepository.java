package com.falconenergy.repository;

import com.falconenergy.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumber(String licenseNumber);
}
