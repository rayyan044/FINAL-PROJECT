package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.exception.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.FlutterwaveClient;
import com.falconenergy.service.InvoiceService;
import com.falconenergy.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** All Flutterwave outcomes pass through verifyAndApply; no caller can settle an invoice directly. */
@Slf4j @Service @RequiredArgsConstructor @Transactional
public class PaymentServiceImpl implements PaymentService {
 private static final Set<PaymentStatus> RECONCILABLE=EnumSet.of(PaymentStatus.INITIATED,PaymentStatus.PENDING,PaymentStatus.PROCESSING,PaymentStatus.ACTION_REQUIRED,PaymentStatus.UNKNOWN);
 private final InvoiceRepository invoices; private final PaymentRepository payments; private final UserRepository users;
 private final InvoiceService invoiceService; private final FlutterwaveClient flutterwave; private final PaymentWebhookEventRepository webhookEvents;

 private Customer customer(){String n=SecurityContextHolder.getContext().getAuthentication().getName(); User u=users.findByEmail(n).or(()->users.findByUsername(n)).orElseThrow(()->new AccessDeniedException("Account not found.")); if(u.getRole()!=UserRole.CUSTOMER||u.getCustomer()==null)throw new AccessDeniedException("Customer account required.");return u.getCustomer();}
 private Invoice customerInvoice(Long id){Customer c=customer();return invoices.findByIdAndOrderCustomerId(id,c.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));}

 @Override public PaymentResponse initiatePawaPayDeposit(Long invoiceId,PawaPayDepositRequest request){
  Invoice owned=customerInvoice(invoiceId); Invoice invoice=invoices.findByIdForUpdate(owned.getId()).orElseThrow(()->new ResourceNotFoundException("Invoice not found."));
  if("PAID".equalsIgnoreCase(invoice.getPaymentStatus()))throw new BadRequestException("Invoice is already paid.");
  if(invoice.getGrandTotal()==null||invoice.getGrandTotal().signum()<=0)throw new BadRequestException("Invoice amount must be greater than zero.");
  if(!"PENDING_PAYMENT".equalsIgnoreCase(invoice.getPaymentStatus())||!"SALES_CONFIRMED".equals(invoice.getOrder().getOrderStatus()))throw new BadRequestException("Invoice is not at a valid stage for payment.");
  String network=network(request.paymentMethod()), phone=normalizeTanzanianPhone(request.phoneNumber()), reference="FLW"+UUID.randomUUID().toString().replace("-","");
  Payment payment=payments.save(Payment.builder().invoice(invoice).paymentReference(reference).gateway("FLUTTERWAVE").paymentMethod(request.paymentMethod()).mobileMoneyNetwork(network).phoneNumber(phone).amount(invoice.getGrandTotal()).currency("TZS").gatewayTransactionId(reference).status(PaymentStatus.INITIATED).gatewayStatus("initiated").initiatedAt(LocalDateTime.now()).build());
  try { FlutterwaveClient.ChargeResult charge=flutterwave.createMobileMoneyCharge(email(customer()),customer().getContactPerson(),phone,network,payment.getAmount(),"TZS",reference); applyProvider(payment,charge); if(isSuccessful(charge.status())) verifyAndApply(payment,charge.id(),"Flutterwave initiation verification"); }
  catch(FlutterwaveException ex){fail(payment,ex.getMessage()); log.warn("Flutterwave initiation failed invoice={} stage={} providerStatus={}",invoice.getId(),ex.getStage(),ex.getHttpStatus());}
  catch(RuntimeException ex){fail(payment,"Payment request could not be sent. Please try again."); log.warn("Flutterwave initiation failed invoice={} type={}",invoice.getId(),ex.getClass().getSimpleName());}
  return map(payment);
 }

 @Override public PaymentResponse processFlutterwaveWebhook(String eventId,String eventType,String chargeId,String reference){
  if(eventId==null||eventId.isBlank()||reference==null||reference.isBlank())throw new BadRequestException("Invalid Flutterwave webhook.");
  if(webhookEvents.existsByGatewayAndEventId("FLUTTERWAVE",eventId)){Payment p=payments.findByPaymentReference(reference).orElseThrow(()->new ResourceNotFoundException("Unknown Flutterwave payment."));return map(p);}
  Payment p=payments.findByReferenceForUpdate(reference).orElseThrow(()->new ResourceNotFoundException("Unknown Flutterwave payment."));
  webhookEvents.save(PaymentWebhookEvent.builder().gateway("FLUTTERWAVE").eventId(eventId).eventType(eventType).payment(p).receivedAt(LocalDateTime.now()).build());
  if(p.getStatus()!=PaymentStatus.SUCCESSFUL && chargeId!=null&&!chargeId.isBlank()) verifyAndApply(p,chargeId,"Flutterwave webhook");
  return map(p);
 }

 @Override public PaymentResponse refreshForCustomer(Long paymentId){ Payment p=payments.findByIdAndInvoiceOrderCustomerId(paymentId,customer().getId()).orElseThrow(()->new ResourceNotFoundException("Payment not found.")); return refresh(p.getId()); }
 @Override public PaymentResponse refreshForStaff(Long paymentId){ if(!hasStaffRole())throw new AccessDeniedException("Finance or administrator access required."); return refresh(paymentId); }
 private PaymentResponse refresh(Long id){Payment p=payments.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Payment not found."));if(!"FLUTTERWAVE".equals(p.getGateway())||isFinal(p.getStatus()))return map(p);try{verifyAndApply(p,p.getGatewayTransactionId(),"Flutterwave status refresh");}catch(FlutterwaveException ex){p.setLastCheckedAt(LocalDateTime.now());log.warn("Flutterwave status refresh unavailable payment={} stage={}",p.getId(),ex.getStage());}return map(p);}
 @Override public PaymentResponse cancelForCustomer(Long id){Payment p=payments.findByIdAndInvoiceOrderCustomerId(id,customer().getId()).orElseThrow(()->new ResourceNotFoundException("Payment not found.")); if(p.getStatus()==PaymentStatus.SUCCESSFUL)throw new BadRequestException("A verified successful payment cannot be cancelled."); if(!isFinal(p.getStatus())){p.setStatus(PaymentStatus.CANCELLED);p.setGatewayStatus("locally_cancelled");p.setFailureReason("Payment was cancelled before confirmation.");p.setLastCheckedAt(LocalDateTime.now());}return map(p);}
 @Override @Scheduled(fixedDelayString="${payments.flutterwave.reconciliation-delay-ms:300000}") public void reconcileOutstandingFlutterwavePayments(){for(Payment p:payments.findTop100ByGatewayAndStatusInOrderByUpdatedAtAsc("FLUTTERWAVE",RECONCILABLE)){try{refresh(p.getId());}catch(RuntimeException ex){log.debug("Deferred Flutterwave reconciliation payment={}",p.getId());}}}

 private void verifyAndApply(Payment p,String chargeId,String source){
  if(chargeId==null||chargeId.isBlank())throw new FlutterwaveException("charge retrieval",null,null,"Flutterwave did not provide a transaction ID.",null,null);
  FlutterwaveClient.ChargeResult charge=flutterwave.retrieveCharge(chargeId);
  if(!p.getPaymentReference().equals(charge.reference())||charge.amount()==null||charge.amount().compareTo(p.getAmount())!=0||!"TZS".equalsIgnoreCase(charge.currency()))throw new FlutterwaveException("charge verification",null,null,"Flutterwave transaction details do not match this invoice.",null,null);
  applyProvider(p,charge); p.setLastCheckedAt(LocalDateTime.now());
  if(isSuccessful(charge.status())){p.setStatus(PaymentStatus.SUCCESSFUL);p.setFailureReason(null);p.setCompletedAt(LocalDateTime.now());p.setVerifiedAt(LocalDateTime.now());if(!"PAID".equalsIgnoreCase(p.getInvoice().getPaymentStatus()))invoiceService.confirmSuccessfulPayment(p.getInvoice().getId(),source);}
 }
 private void applyProvider(Payment p,FlutterwaveClient.ChargeResult charge){
  if(charge.id()!=null&&!charge.id().isBlank())p.setGatewayTransactionId(charge.id()); p.setProviderReference(clean(charge.providerReference()));p.setGatewayStatus(clean(charge.status()));p.setNextAction(clean(charge.nextActionType()));p.setAuthorizationUrl(cleanUrl(charge.redirectUrl()));p.setAuthorizationInstruction(clean(charge.instruction()));
  if(!isSuccessful(charge.status())){PaymentStatus next=mapStatus(charge.status(),charge.nextActionType(),charge.failureReason()); if(!(p.getStatus()==PaymentStatus.SUCCESSFUL))p.setStatus(next); if(isNegative(next))p.setFailureReason(clean(charge.failureReason())==null?friendly(next):clean(charge.failureReason()));}
 }
 private void fail(Payment p,String message){if(p.getStatus()!=PaymentStatus.SUCCESSFUL){p.setStatus(PaymentStatus.FAILED);p.setGatewayStatus("failed");p.setFailureReason(clean(message));p.setLastCheckedAt(LocalDateTime.now());}}
 private static boolean isSuccessful(String value){return "succeeded".equalsIgnoreCase(value)||"successful".equalsIgnoreCase(value)||"completed".equalsIgnoreCase(value);}
 private static boolean isFinal(PaymentStatus s){return s==PaymentStatus.SUCCESSFUL||s==PaymentStatus.COMPLETED||s==PaymentStatus.FAILED||s==PaymentStatus.CANCELLED||s==PaymentStatus.EXPIRED||s==PaymentStatus.REVERSED;}
 private static boolean isNegative(PaymentStatus s){return s==PaymentStatus.FAILED||s==PaymentStatus.CANCELLED||s==PaymentStatus.EXPIRED;}
 static PaymentStatus mapStatus(String raw,String action){return mapStatus(raw,action,null);}
 static PaymentStatus mapStatus(String raw,String action,String reason){String s=raw==null?"":raw.toLowerCase(Locale.ROOT);String detail=((action==null?"":action)+" "+(reason==null?"":reason)).toLowerCase(Locale.ROOT);if(isSuccessful(s))return PaymentStatus.SUCCESSFUL;if(s.contains("cancel")||detail.contains("cancel"))return PaymentStatus.CANCELLED;if("failed".equals(s)||"declined".equals(s)||"error".equals(s))return PaymentStatus.FAILED;if("expired".equals(s))return PaymentStatus.EXPIRED;if("processing".equals(s)||"in_progress".equals(s))return PaymentStatus.PROCESSING;if("pending".equals(s)&&action!=null&&!action.isBlank())return PaymentStatus.ACTION_REQUIRED;if("pending".equals(s)||"initiated".equals(s))return PaymentStatus.PENDING;return PaymentStatus.UNKNOWN;}
 static String normalizeTanzanianPhone(String value){String phone=value==null?"":value.replaceAll("[^0-9]","");if(phone.startsWith("0"))phone="255"+phone.substring(1);if(!phone.startsWith("255")||phone.length()!=12||!phone.substring(3).matches("[67]\\d{8}"))throw new BadRequestException("Enter a valid Tanzanian mobile number.");return phone;}
 private static String network(String method){return switch(method){case "AIRTEL_MONEY"->"AIRTEL";case "MIXX_BY_YAS"->"TIGO";case "HALOPESA"->"HALOPESA";case "VODACOM_MONEY"->"VODACOM";default->throw new BadRequestException("Unsupported Tanzania mobile-money provider.");};}
 private static String email(Customer c){return c.getEmail()==null||c.getEmail().isBlank()?"customer-"+c.getId()+"@falcon.local":c.getEmail();}
 private static String friendly(PaymentStatus s){return switch(s){case CANCELLED->"Payment was cancelled.";case EXPIRED->"Payment expired before confirmation.";default->"Payment failed. Please try again.";};}
 private static String clean(String v){if(v==null)return null;String x=v.replaceAll("[\\r\\n\\t]"," ").trim();return x.isBlank()?null:(x.length()>300?x.substring(0,300):x);}
 private static String cleanUrl(String value){return value!=null&&value.startsWith("https://")?value:null;}
 private boolean hasStaffRole(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getAuthorities().stream().anyMatch(x->Set.of("ROLE_FINANCE","ROLE_ADMIN","ROLE_MANAGER").contains(x.getAuthority()));}
 @Override public List<PaymentResponse> listForCustomer(Long invoiceId){customerInvoice(invoiceId);return listForInvoice(invoiceId);}
 @Override public List<PaymentResponse> listForInvoice(Long invoiceId){return payments.findByInvoiceIdOrderByCreatedAtDesc(invoiceId).stream().map(this::map).toList();}
 private PaymentResponse map(Payment p){Invoice i=p.getInvoice();return new PaymentResponse(p.getId(),p.getPaymentReference(),p.getStatus(),p.getAmount(),p.getCurrency(),p.getPaymentMethod(),p.getMobileMoneyNetwork(),mask(p.getPhoneNumber()),p.getProviderReference(),p.getFailureReason(),p.getGatewayStatus(),p.getNextAction(),p.getAuthorizationUrl(),p.getAuthorizationInstruction(),p.getInitiatedAt(),p.getUpdatedAt(),p.getCompletedAt(),p.getVerifiedAt(),i.getId(),i.getPaymentStatus(),i.getOrder().getOrderStatus());}
 private static String mask(String phone){return phone==null?null:phone.length()<5?"***":"+"+phone.substring(0,3)+"***"+phone.substring(phone.length()-3);}
 @Override public PaymentResponse processPawaPayDepositCallback(PawaPayDepositCallback callback){throw new BadRequestException("pawaPay callbacks are not handled by the Flutterwave mobile-money flow.");}
}
