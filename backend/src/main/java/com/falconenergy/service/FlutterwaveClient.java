package com.falconenergy.service;

import java.math.BigDecimal;

public interface FlutterwaveClient {
    ChargeResult createMobileMoneyCharge(String email, String name, String phoneNumber, String network, BigDecimal amount, String currency, String reference);
    ChargeResult retrieveCharge(String chargeId);
    record ChargeResult(String id, String reference, BigDecimal amount, String currency, String status, String nextActionType, String redirectUrl, String failureReason) { }
}
