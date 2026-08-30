package com.falconenergy.dto;
import com.falconenergy.entity.PaymentStatus; import java.math.BigDecimal; import java.time.LocalDateTime;
public record PaymentResponse(Long id,String paymentReference,PaymentStatus status,BigDecimal amount,String currency,String paymentMethod,String phoneNumber,String failureReason,LocalDateTime initiatedAt,LocalDateTime completedAt,Long invoiceId,String invoicePaymentStatus,String orderStatus) {}
