package com.falconenergy.repository;

import com.falconenergy.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCustomerCode(String customerCode);
    boolean existsByCustomerCode(String customerCode);
    boolean existsByEmail(String email);
}
