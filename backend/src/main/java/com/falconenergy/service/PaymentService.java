package com.falconenergy.service;
import com.falconenergy.dto.*; import java.util.*;
public interface PaymentService {
 PaymentResponse initiatePawaPayDeposit(Long invoiceId, PawaPayDepositRequest request);
 PaymentResponse processPawaPayDepositCallback(PawaPayDepositCallback callback);
 PaymentResponse processFlutterwaveWebhook(String eventId, String eventType, String chargeId, String reference);
 PaymentResponse refreshForCustomer(Long paymentId);
 PaymentResponse refreshForStaff(Long paymentId);
 PaymentResponse cancelForCustomer(Long paymentId);
 List<PaymentResponse> listForCustomer(Long invoiceId);
 List<PaymentResponse> listForInvoice(Long invoiceId);
 void reconcileOutstandingFlutterwavePayments();
}
