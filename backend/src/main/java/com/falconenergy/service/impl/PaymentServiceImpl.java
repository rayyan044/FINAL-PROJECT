package com.falconenergy.service.impl;
import com.falconenergy.config.PawaPayProperties; import com.falconenergy.dto.*; import com.falconenergy.entity.*; import com.falconenergy.exception.*; import com.falconenergy.repository.*; import com.falconenergy.service.InvoiceService; import com.falconenergy.service.PaymentService; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*; import java.util.*;
@Slf4j @Service @RequiredArgsConstructor @Transactional public class PaymentServiceImpl implements PaymentService {
 private final InvoiceRepository invoices; private final PaymentRepository payments; private final UserRepository users; private final InvoiceService invoiceService; private final com.falconenergy.service.PawaPayClient pawaPay; private final PawaPayProperties pawaPayProperties;
 private Customer customer(){String n=SecurityContextHolder.getContext().getAuthentication().getName(); User u=users.findByEmail(n).or(()->users.findByUsername(n)).orElseThrow(()->new AccessDeniedException("Account not found.")); if(u.getRole()!=UserRole.CUSTOMER||u.getCustomer()==null)throw new AccessDeniedException("Customer account required.");return u.getCustomer();}
 private Invoice invoice(Long id){Customer c=customer();return invoices.findByIdAndOrderCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));}
 public PaymentResponse initiatePawaPayDeposit(Long id, PawaPayDepositRequest request){
  Invoice owned=invoice(id); // ownership is derived exclusively from the authenticated customer
  Invoice i=invoices.findByIdForUpdate(owned.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));
  if("PAID".equalsIgnoreCase(i.getPaymentStatus())) throw new BadRequestException("Invoice is already paid.");
  if(i.getGrandTotal()==null||i.getGrandTotal().signum()<=0) throw new BadRequestException("Invoice amount must be greater than zero.");
  if(!"PENDING_PAYMENT".equalsIgnoreCase(i.getPaymentStatus())||!"SALES_CONFIRMED".equals(i.getOrder().getOrderStatus())) throw new BadRequestException("Invoice is not at a valid stage for payment.");
  String correspondent=correspondent(request.paymentMethod()); String phone=phone(request.phoneNumber()); UUID depositId=UUID.randomUUID(); LocalDateTime now=LocalDateTime.now();
  // Tanzania mobile-money providers settle deposits in Tanzanian shillings. A legacy order can
  // carry USD while its invoice total is displayed in TZS; passing that currency to pawaPay
  // causes the gateway to reject an otherwise valid deposit.
  String currency=correspondent.endsWith("_TZA")?"TZS":(i.getOrder().getCurrency()==null?"TZS":i.getOrder().getCurrency());
  Payment p=payments.save(Payment.builder().invoice(i).paymentReference("PWP-"+depositId).gateway("PAWAPAY").paymentMethod(request.paymentMethod()).phoneNumber(phone).amount(i.getGrandTotal()).currency(currency).gatewayTransactionId(depositId.toString()).status(PaymentStatus.PENDING).initiatedAt(now).build());
  // The configured sandbox cannot charge a real phone. For local demonstrations, a valid
  // payment is settled immediately so users can test the complete order workflow.
  if("sandbox".equalsIgnoreCase(pawaPayProperties.getEnvironment())) { p.setStatus(PaymentStatus.COMPLETED); p.setCompletedAt(now); invoiceService.confirmSuccessfulPayment(i.getId(),"sandbox demo payment"); return map(p); }
  try { com.falconenergy.service.PawaPayClient.DepositResult result=pawaPay.initiateDeposit(depositId,p.getAmount(),p.getCurrency(),phone,correspondent,i.getInvoiceNumber()); if("ACCEPTED".equals(result.status())||"DUPLICATE_IGNORED".equals(result.status())) p.setStatus(PaymentStatus.PROCESSING); else {p.setStatus(PaymentStatus.FAILED);p.setFailureReason(result.failureMessage()==null?"pawaPay rejected the deposit.":result.failureMessage());} }
  catch(RuntimeException exception){p.setStatus(PaymentStatus.FAILED);p.setFailureReason(exception.getMessage()==null?"Unable to initiate pawaPay payment.":exception.getMessage());log.warn("pawaPay deposit initiation failed for invoice={}: {}",i.getId(),exception.getMessage());}
  return map(p);
 }
 public PaymentResponse processPawaPayDepositCallback(PawaPayDepositCallback callback){
  Payment payment=payments.findByGatewayTransactionIdForUpdate(callback.depositId()).orElseThrow(()->new ResourceNotFoundException("Unknown pawaPay deposit."));
  if(!"PAWAPAY".equalsIgnoreCase(payment.getGateway())) throw new BadRequestException("Payment is not a pawaPay deposit.");
  if(callback.amount()==null||callback.amount().compareTo(payment.getAmount())!=0||!payment.getCurrency().equalsIgnoreCase(callback.currency())) throw new BadRequestException("pawaPay callback amount or currency does not match the payment.");
  if(payment.getStatus()==PaymentStatus.COMPLETED) return map(payment); // pawaPay may retry a callback
  LocalDateTime now=LocalDateTime.now();
  if("COMPLETED".equalsIgnoreCase(callback.status())){
   payment.setStatus(PaymentStatus.COMPLETED); payment.setCompletedAt(now); payment.setFailureReason(null);
   Invoice invoice=payment.getInvoice();
   if(!"PAID".equalsIgnoreCase(invoice.getPaymentStatus())) invoiceService.confirmSuccessfulPayment(invoice.getId(),"pawaPay callback");
  }else if("FAILED".equalsIgnoreCase(callback.status())){
   payment.setStatus(PaymentStatus.FAILED); payment.setFailureReason(callback.failureMessage()==null?"pawaPay deposit failed.":callback.failureMessage());
  }else if("PROCESSING".equalsIgnoreCase(callback.status())) payment.setStatus(PaymentStatus.PROCESSING);
  else throw new BadRequestException("Unsupported pawaPay deposit status.");
  return map(payment);
 }
 private String correspondent(String method){return switch(method){case "AIRTEL_MONEY"->"AIRTEL_TZA";case "MIXX_BY_YAS"->"TIGO_TZA";case "HALOPESA"->"HALOTEL_TZA";default->throw new BadRequestException("Unsupported pawaPay payment method.");};}
 private String phone(String value){String phone=value.replaceAll("[^0-9]","");if(!phone.startsWith("255")||phone.length()!=12)throw new BadRequestException("Enter a Tanzanian mobile number with country code 255.");return phone;}
 public List<PaymentResponse> listForCustomer(Long id){invoice(id);return payments.findByInvoiceIdOrderByCreatedAtDesc(id).stream().map(this::map).toList();}
 private PaymentResponse map(Payment p){Invoice i=p.getInvoice();return new PaymentResponse(p.getId(),p.getPaymentReference(),p.getStatus(),p.getAmount(),p.getCurrency(),p.getPaymentMethod(),p.getPhoneNumber(),p.getFailureReason(),p.getInitiatedAt(),p.getCompletedAt(),i.getId(),i.getPaymentStatus(),i.getOrder().getOrderStatus());}
}
