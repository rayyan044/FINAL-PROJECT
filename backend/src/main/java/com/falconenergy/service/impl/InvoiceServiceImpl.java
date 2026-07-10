package com.falconenergy.service.impl;

import com.falconenergy.dto.InvoiceResponse;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.entity.PaymentAccount;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.mapper.InvoiceMapper;
import com.falconenergy.mapper.CompanySettingsMapper;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.repository.CompanySettingsRepository;
import com.falconenergy.repository.PaymentAccountRepository;
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
    private final CompanySettingsRepository companySettingsRepository;
    private final CompanySettingsMapper companySettingsMapper;
    private final PaymentAccountRepository paymentAccountRepository;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            AuditLogService auditLogService,
            CompanySettingsRepository companySettingsRepository,
            CompanySettingsMapper companySettingsMapper,
            PaymentAccountRepository paymentAccountRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.auditLogService = auditLogService;
        this.companySettingsRepository = companySettingsRepository;
        this.companySettingsMapper = companySettingsMapper;
        this.paymentAccountRepository = paymentAccountRepository;
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        InvoiceResponse response = invoiceMapper.toResponse(invoice);
        if (response != null) {
            // Determine invoice type
            if ("PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
                response.setInvoiceType("Invoice");
            } else {
                response.setInvoiceType("Proforma Invoice");
            }
            
            // Retrieve company details
            CompanySettings companySettings = companySettingsRepository.findFirstByOrderByIdAsc()
                    .orElseGet(() -> CompanySettings.builder()
                            .companyName("FALCON ENERGY LIMITED")
                            .postalAddress("P.O. Box : 45431, 6th Floor, SALAMANDER TOWER")
                            .officeAddress("SAMORA AVENUE, DAR ES SALAAM")
                            .phoneNumber("+255 22 212 3456")
                            .email("info@falconenergy.co.tz")
                            .logo("assets/falcon-logo.png")
                            .signatoryName("AUTHORIZED SIGNATORY")
                            .signatoryTitle("FINANCE CONTROLLER")
                            .signatorySignature("assets/authorized-signature.png")
                            .build());
            response.setCompanyDetails(companySettingsMapper.toResponse(companySettings));
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        log.info("Fetching all invoices");
        return invoiceRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        log.info("Fetching invoice by id: {}", id);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return mapToResponse(invoice);
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

        return mapToResponse(updated);
    }

    @Override
    public InvoiceResponse overrideInvoiceStatus(Long id, String status, String updatedBy) {
        log.info("Overriding payment status for invoice: {} to {} by {}", id, status, updatedBy);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        String oldStatus = invoice.getPaymentStatus();
        invoice.setPaymentStatus(status.toUpperCase());
        if ("PAID".equalsIgnoreCase(status)) {
            invoice.setFinanceApprovedBy(updatedBy != null && !updatedBy.isBlank() ? updatedBy : "admin");
            invoice.setFinanceApprovedAt(LocalDateTime.now());
        } else {
            invoice.setFinanceApprovedBy(null);
            invoice.setFinanceApprovedAt(null);
        }

        Invoice updated = invoiceRepository.save(invoice);

        auditLogService.log(
                "INVOICE_OVERRIDE",
                "INVOICE",
                updated.getId(),
                updated.getOrder().getCustomer().getCustomerCode(),
                "Invoice status overridden from " + oldStatus + " to " + status.toUpperCase() + " by " + updatedBy
        );

        return mapToResponse(updated);
    }

    @Override
    public InvoiceResponse updateInvoicePaymentAccount(Long id, Long paymentAccountId, String updatedBy) {
        log.info("Updating payment account snapshot for invoice: {} to payment account: {} by {}", id, paymentAccountId, updatedBy);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (!"PENDING_PAYMENT".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new BadRequestException("Cannot update payment account instructions on a paid or cancelled invoice");
        }

        PaymentAccount paymentAccount = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with id: " + paymentAccountId));

        if (!"ACTIVE".equalsIgnoreCase(paymentAccount.getStatus())) {
            throw new BadRequestException("Cannot use an inactive payment account");
        }

        // Update snapshot
        invoice.setPaymentAccountId(paymentAccount.getId());
        invoice.setPaymentMethod(paymentAccount.getPaymentMethod());
        invoice.setBeneficiaryName(paymentAccount.getBeneficiaryName());
        invoice.setBankName(paymentAccount.getBankName());
        invoice.setBranchName(paymentAccount.getBranchName());
        invoice.setAccountNumber(paymentAccount.getAccountNumber());
        invoice.setSwiftCode(paymentAccount.getSwiftCode());
        invoice.setPaymentAccountCurrency(paymentAccount.getCurrency());
        invoice.setPaymentTerms(paymentAccount.getPaymentTerms());
        invoice.setPaymentInstructions(paymentAccount.getPaymentInstructions());
        
        // Recalculate validity date based on new payment account
        int validityDays = paymentAccount.getValidityDays() != null ? paymentAccount.getValidityDays() : 30;
        invoice.setValidityDate(invoice.getInvoiceDate().plusDays(validityDays));

        Invoice updated = invoiceRepository.save(invoice);

        auditLogService.log(
                "INVOICE_PAYMENT_ACCOUNT_UPDATED",
                "INVOICE",
                updated.getId(),
                invoice.getOrder().getCustomer().getCustomerCode(),
                "Invoice payment account instructions updated to " + paymentAccount.getBankName() + " (Acc: " + paymentAccount.getAccountNumber() + ") by " + updatedBy
        );

        return mapToResponse(updated);
    }
}
