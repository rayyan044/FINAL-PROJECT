package com.falconenergy.repository;

import com.falconenergy.entity.FuelTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelTransactionRepository extends JpaRepository<FuelTransaction, Long>, JpaSpecificationExecutor<FuelTransaction> {
    Optional<FuelTransaction> findByTransactionNumber(String transactionNumber);
    boolean existsByTransactionNumber(String transactionNumber);
}
