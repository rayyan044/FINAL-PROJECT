package com.falconenergy.service.impl;

import com.falconenergy.dto.InvoiceResponse;
import com.falconenergy.entity.Invoice;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.InvoiceMapper;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.service.InvoiceService;
import com.falconenergy.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final AuditLogService auditLogService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            AuditLogService auditLogService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        log.info("Fetching all invoices");
        return invoiceRepository.findAll(pageable).map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        log.info("Fetching invoice by id: {}", id);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse approveInvoicePayment(Long id, String approvedBy) {
        log.info("Approving payment for invoice: {} by {}", id, approvedBy);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        invoice.setPaymentStatus("PAID");
        invoice.setFinanceApprovedBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy : "finance_officer");
        invoice.setFinanceApprovedAt(LocalDateTime.now());

        Invoice updated = invoiceRepository.save(invoice);

        auditLogService.log(
                "INVOICE_PAID",
                "INVOICE",
                updated.getId(),
                updated.getOrder().getCustomer().getCustomerCode(),
                "Invoice payment approved by " + approvedBy
        );

        return invoiceMapper.toResponse(updated);
    }
}
