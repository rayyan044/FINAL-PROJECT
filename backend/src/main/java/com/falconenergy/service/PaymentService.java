package com.falconenergy.service;
import com.falconenergy.dto.*; import java.util.*;
public interface PaymentService { PaymentResponse initiatePawaPayDeposit(Long invoiceId, PawaPayDepositRequest request); PaymentResponse processPawaPayDepositCallback(PawaPayDepositCallback callback); PaymentResponse processFlutterwaveWebhook(String chargeId, String reference, String status, java.math.BigDecimal amount, String currency); List<PaymentResponse> listForCustomer(Long invoiceId); }
