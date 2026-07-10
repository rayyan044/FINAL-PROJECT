package com.falconenergy.service.impl;

import com.falconenergy.dto.PaymentAccountRequest;
import com.falconenergy.dto.PaymentAccountResponse;
import com.falconenergy.entity.PaymentAccount;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.PaymentAccountMapper;
import com.falconenergy.repository.PaymentAccountRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.PaymentAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PaymentAccountServiceImpl implements PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentAccountMapper paymentAccountMapper;
    private final AuditLogService auditLogService;

    public PaymentAccountServiceImpl(
            PaymentAccountRepository paymentAccountRepository,
            PaymentAccountMapper paymentAccountMapper,
            AuditLogService auditLogService
    ) {
        this.paymentAccountRepository = paymentAccountRepository;
        this.paymentAccountMapper = paymentAccountMapper;
        this.auditLogService = auditLogService;
    }

    private String resolveCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "system";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentAccountResponse> getAllPaymentAccounts(Pageable pageable) {
        log.info("Fetching all payment accounts");
        return paymentAccountRepository.findAll(pageable).map(paymentAccountMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAccountResponse getPaymentAccountById(Long id) {
        log.info("Fetching payment account by id: {}", id);
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with id: " + id));
        return paymentAccountMapper.toResponse(account);
    }

    @Override
    public PaymentAccountResponse createPaymentAccount(PaymentAccountRequest request) {
        log.info("Creating new payment account: {} - {}", request.getBankName(), request.getAccountNumber());
        PaymentAccount account = paymentAccountMapper.toEntity(request);
        if (account.getStatus() == null) {
            account.setStatus("ACTIVE");
        }
        if (account.getValidityDays() == null) {
            account.setValidityDays(30);
        }
        PaymentAccount saved = paymentAccountRepository.save(account);

        auditLogService.log(
                "PAYMENT_ACCOUNT_CREATED",
                "PAYMENT_ACCOUNT",
                saved.getId(),
                resolveCurrentUser(),
                String.format("Payment account created: %s, %s, Acc: %s", saved.getBankName(), saved.getBeneficiaryName(), saved.getAccountNumber())
        );

        return paymentAccountMapper.toResponse(saved);
    }

    @Override
    public PaymentAccountResponse updatePaymentAccount(Long id, PaymentAccountRequest request) {
        log.info("Updating payment account with id: {}", id);
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with id: " + id));

        String oldDetails = String.format("%s, %s, Acc: %s, Status: %s", account.getBankName(), account.getBeneficiaryName(), account.getAccountNumber(), account.getStatus());
        paymentAccountMapper.updateEntityFromRequest(request, account);
        PaymentAccount saved = paymentAccountRepository.save(account);

        String newDetails = String.format("%s, %s, Acc: %s, Status: %s", saved.getBankName(), saved.getBeneficiaryName(), saved.getAccountNumber(), saved.getStatus());

        auditLogService.log(
                "PAYMENT_ACCOUNT_UPDATED",
                "PAYMENT_ACCOUNT",
                saved.getId(),
                resolveCurrentUser(),
                String.format("Payment account updated. Old: [%s], New: [%s]", oldDetails, newDetails)
        );

        return paymentAccountMapper.toResponse(saved);
    }

    @Override
    public void deletePaymentAccount(Long id) {
        log.info("Deleting payment account with id: {}", id);
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with id: " + id));
        paymentAccountRepository.delete(account);

        auditLogService.log(
                "PAYMENT_ACCOUNT_DELETED",
                "PAYMENT_ACCOUNT",
                id,
                resolveCurrentUser(),
                String.format("Payment account deleted: %s, %s, Acc: %s", account.getBankName(), account.getBeneficiaryName(), account.getAccountNumber())
        );
    }

    @Override
    public PaymentAccountResponse toggleStatus(Long id) {
        log.info("Toggling status of payment account with id: {}", id);
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found with id: " + id));

        String oldStatus = account.getStatus();
        String newStatus = "ACTIVE".equalsIgnoreCase(oldStatus) ? "INACTIVE" : "ACTIVE";
        account.setStatus(newStatus);
        PaymentAccount saved = paymentAccountRepository.save(account);

        auditLogService.log(
                "PAYMENT_ACCOUNT_STATUS_TOGGLED",
                "PAYMENT_ACCOUNT",
                saved.getId(),
                resolveCurrentUser(),
                String.format("Payment account status toggled from %s to %s", oldStatus, newStatus)
        );

        return paymentAccountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> getActivePaymentAccounts() {
        log.info("Fetching active payment accounts");
        return paymentAccountRepository.findByStatus("ACTIVE").stream()
                .map(paymentAccountMapper::toResponse)
                .collect(Collectors.toList());
    }
}
