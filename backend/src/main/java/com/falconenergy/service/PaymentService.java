package com.falconenergy.service;
import com.falconenergy.dto.*; import java.util.*;
public interface PaymentService { PaymentResponse initiatePawaPayDeposit(Long invoiceId, PawaPayDepositRequest request); PaymentResponse processPawaPayDepositCallback(PawaPayDepositCallback callback); List<PaymentResponse> listForCustomer(Long invoiceId); }
