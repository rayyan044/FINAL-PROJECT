package com.falconenergy.service.impl;
import com.falconenergy.dto.*; import com.falconenergy.entity.*; import com.falconenergy.exception.*; import com.falconenergy.repository.*; import com.falconenergy.service.InvoiceService; import com.falconenergy.service.PaymentService; import com.falconenergy.service.FlutterwaveClient; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.math.BigDecimal; import java.time.*; import java.util.*;
@Slf4j @Service @RequiredArgsConstructor @Transactional public class PaymentServiceImpl implements PaymentService {
 private final InvoiceRepository invoices; private final PaymentRepository payments; private final UserRepository users; private final InvoiceService invoiceService; private final FlutterwaveClient flutterwave;
 private Customer customer(){String n=SecurityContextHolder.getContext().getAuthentication().getName(); User u=users.findByEmail(n).or(()->users.findByUsername(n)).orElseThrow(()->new AccessDeniedException("Account not found.")); if(u.getRole()!=UserRole.CUSTOMER||u.getCustomer()==null)throw new AccessDeniedException("Customer account required.");return u.getCustomer();}
 private Invoice invoice(Long id){Customer c=customer();return invoices.findByIdAndOrderCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));}
 public PaymentResponse initiatePawaPayDeposit(Long id, PawaPayDepositRequest request){
  Invoice owned=invoice(id); // ownership is derived exclusively from the authenticated customer
  Invoice i=invoices.findByIdForUpdate(owned.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));
  if("PAID".equalsIgnoreCase(i.getPaymentStatus())) throw new BadRequestException("Invoice is already paid.");
  if(i.getGrandTotal()==null||i.getGrandTotal().signum()<=0) throw new BadRequestException("Invoice amount must be greater than zero.");
  if(!"PENDING_PAYMENT".equalsIgnoreCase(i.getPaymentStatus())||!"SALES_CONFIRMED".equals(i.getOrder().getOrderStatus())) throw new BadRequestException("Invoice is not at a valid stage for payment.");
  String network=network(request.paymentMethod()); String phone=phone(request.phoneNumber()); String currency="TZS"; String reference="FLW-"+UUID.randomUUID(); LocalDateTime now=LocalDateTime.now();
  Payment p=payments.save(Payment.builder().invoice(i).paymentReference(reference).gateway("FLUTTERWAVE").paymentMethod(request.paymentMethod()).phoneNumber(phone).amount(i.getGrandTotal()).currency(currency).gatewayTransactionId(reference).status(PaymentStatus.PENDING).gatewayStatus("pending").initiatedAt(now).build());
  try { FlutterwaveClient.ChargeResult charge=flutterwave.createMobileMoneyCharge(customer().getEmail()==null?"customer-"+customer().getId()+"@falcon.local":customer().getEmail(),customer().getContactPerson(),phone,network,p.getAmount(),currency,reference); p.setGatewayTransactionId(charge.id()); p.setGatewayStatus(charge.status()); p.setNextAction(charge.nextActionType()); p.setAuthorizationUrl(charge.redirectUrl()); applyStatus(p, charge.status(), charge.failureReason()); if(p.getStatus()==PaymentStatus.COMPLETED) settle(p,"Flutterwave charge creation"); }
  catch(RuntimeException exception){p.setStatus(PaymentStatus.FAILED);p.setGatewayStatus("failed");p.setFailureReason("Unable to initiate Flutterwave payment.");log.warn("Flutterwave charge initiation failed for invoice={}: {}",i.getId(),exception.getMessage());}
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
 public PaymentResponse processFlutterwaveWebhook(String chargeId,String reference,String status,BigDecimal amount,String currency){
  if(reference==null||reference.isBlank()) throw new BadRequestException("Flutterwave webhook has no payment reference.");
  Payment p=payments.findByReferenceForUpdate(reference).orElseThrow(()->new ResourceNotFoundException("Unknown Flutterwave payment."));
  if(!"FLUTTERWAVE".equalsIgnoreCase(p.getGateway())) throw new BadRequestException("Payment is not a Flutterwave charge.");
  FlutterwaveClient.ChargeResult charge=flutterwave.retrieveCharge(chargeId);
  if(!reference.equals(charge.reference())||charge.amount()==null||charge.amount().compareTo(p.getAmount())!=0||!p.getCurrency().equalsIgnoreCase(charge.currency())) throw new BadRequestException("Flutterwave charge does not match this payment.");
  p.setGatewayTransactionId(charge.id());p.setGatewayStatus(charge.status());p.setNextAction(charge.nextActionType());p.setAuthorizationUrl(charge.redirectUrl());applyStatus(p,charge.status(),charge.failureReason()); if(p.getStatus()==PaymentStatus.COMPLETED) settle(p,"Flutterwave webhook"); return map(p);
 }
 private void settle(Payment p,String source){if(!"PAID".equalsIgnoreCase(p.getInvoice().getPaymentStatus())){p.setCompletedAt(LocalDateTime.now());invoiceService.confirmSuccessfulPayment(p.getInvoice().getId(),source);}}
 private void applyStatus(Payment p,String gatewayStatus,String reason){String value=gatewayStatus==null?"pending":gatewayStatus.toLowerCase(Locale.ROOT);p.setGatewayStatus(value);switch(value){case "succeeded","successful","completed"-> {p.setStatus(PaymentStatus.COMPLETED);p.setFailureReason(null);}case "failed"-> {p.setStatus(PaymentStatus.FAILED);p.setFailureReason(reason==null?"Flutterwave payment failed.":reason);}case "cancelled","canceled"->p.setStatus(PaymentStatus.CANCELLED);case "expired"->p.setStatus(PaymentStatus.EXPIRED);case "reversed"->p.setStatus(PaymentStatus.REVERSED);default->p.setStatus(PaymentStatus.PROCESSING);}}
 private String network(String method){return switch(method){case "AIRTEL_MONEY"->"AIRTEL";case "MIXX_BY_YAS"->"TIGO";case "HALOPESA"->"HALOTEL";default->throw new BadRequestException("Unsupported Flutterwave mobile-money provider.");};}
 private String phone(String value){String phone=value.replaceAll("[^0-9]","");if(!phone.startsWith("255")||phone.length()!=12)throw new BadRequestException("Enter a Tanzanian mobile number with country code 255.");return phone;}
 public List<PaymentResponse> listForCustomer(Long id){invoice(id);return payments.findByInvoiceIdOrderByCreatedAtDesc(id).stream().map(this::map).toList();}
 private PaymentResponse map(Payment p){Invoice i=p.getInvoice();return new PaymentResponse(p.getId(),p.getPaymentReference(),p.getStatus(),p.getAmount(),p.getCurrency(),p.getPaymentMethod(),p.getPhoneNumber(),p.getFailureReason(),p.getGatewayStatus(),p.getNextAction(),p.getAuthorizationUrl(),p.getInitiatedAt(),p.getCompletedAt(),i.getId(),i.getPaymentStatus(),i.getOrder().getOrderStatus());}
}
