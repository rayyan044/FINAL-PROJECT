package com.falconenergy.service;
import com.falconenergy.dto.PaymentReceiptResponse; import com.falconenergy.entity.Invoice;
public interface PaymentReceiptService { PaymentReceiptResponse generateForPaidInvoice(Invoice invoice); PaymentReceiptResponse getByInvoiceId(Long invoiceId); boolean existsForInvoiceId(Long invoiceId); }
