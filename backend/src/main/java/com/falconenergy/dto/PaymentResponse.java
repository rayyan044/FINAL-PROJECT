package com.falconenergy.dto;
import com.falconenergy.entity.PaymentStatus; import java.math.BigDecimal; import java.time.LocalDateTime;
public record PaymentResponse(Long id,String paymentReference,PaymentStatus status,BigDecimal amount,String currency,String paymentMethod,
 String mobileMoneyNetwork,String maskedPhoneNumber,String providerReference,String failureReason,String gatewayStatus,
 String nextAction,String authorizationUrl,String authorizationInstruction,LocalDateTime initiatedAt,LocalDateTime lastUpdatedAt,
 LocalDateTime completedAt,LocalDateTime verifiedAt,Long invoiceId,String invoicePaymentStatus,String orderStatus) {}
