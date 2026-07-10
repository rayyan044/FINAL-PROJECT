package com.falconenergy.repository;

import com.falconenergy.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long>, JpaSpecificationExecutor<PaymentAccount> {
    List<PaymentAccount> findByStatus(String status);
    List<PaymentAccount> findByCurrencyAndStatus(String currency, String status);
}
