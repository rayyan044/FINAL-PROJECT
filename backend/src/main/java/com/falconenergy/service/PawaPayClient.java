package com.falconenergy.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PawaPayClient {
    DepositResult initiateDeposit(UUID depositId, BigDecimal amount, String currency, String phoneNumber, String correspondent, String invoiceNumber);
    String publicKeyPem(String keyId);
    record DepositResult(String status, String failureMessage) { }
}
