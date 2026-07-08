package com.falconenergy.service;

import com.falconenergy.dto.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    Page<InvoiceResponse> getAllInvoices(Pageable pageable);
    InvoiceResponse getInvoiceById(Long id);
    InvoiceResponse approveInvoicePayment(Long id, String approvedBy);
}
