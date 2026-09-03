package com.falconenergy.service.impl;

import com.falconenergy.dto.PaymentResponse;
import com.falconenergy.dto.PawaPayDepositRequest;
import com.falconenergy.dto.PawaPayDepositCallback;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.FlutterwaveException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.InvoiceRepository;
import com.falconenergy.repository.PaymentRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.InvoiceService;
import com.falconenergy.service.FlutterwaveClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock InvoiceRepository invoices; @Mock PaymentRepository payments; @Mock UserRepository users;
    @Mock InvoiceService invoiceService; @Mock FlutterwaveClient flutterwave;
    @InjectMocks PaymentServiceImpl service;
    Customer customer; Invoice invoice;

    @BeforeEach void setUp() {
        customer=Customer.builder().customerCode("C-1").companyName("Customer").build(); customer.setId(7L);
        Role role=Role.builder().roleName("CUSTOMER").build();
        User user=User.builder().username("customer").email("customer@example.com").roleEntity(role).customer(customer).build();
        lenient().when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("customer@example.com", null));
        FuelOrder order=FuelOrder.builder().customer(customer).orderStatus("SALES_CONFIRMED").currency("TZS").build(); order.setId(21L);
        invoice=Invoice.builder().invoiceNumber("INV-1").order(order).paymentStatus("PENDING_PAYMENT").grandTotal(new BigDecimal("6000000.00")).build(); invoice.setId(12L);
    }
    @AfterEach void cleanUp(){ SecurityContextHolder.clearContext(); }

    @Test void customerInitiatesFlutterwaveChargeWithoutSettlingPendingInvoice() {
        when(invoices.findByIdAndOrderCustomerId(12L,7L)).thenReturn(Optional.of(invoice));
        when(invoices.findByIdForUpdate(12L)).thenReturn(Optional.of(invoice));
        when(payments.save(any())).thenAnswer(i -> { Payment p=i.getArgument(0); if(p.getId()==null)p.setId(1L); return p; });
        when(flutterwave.createMobileMoneyCharge(any(),any(),any(),any(),any(),any(),any())).thenReturn(new FlutterwaveClient.ChargeResult("chg_test",null,null,invoice.getGrandTotal(),"TZS","pending","requires_authorization",null,null,null));
        PaymentResponse response=service.initiatePawaPayDeposit(12L,new PawaPayDepositRequest("AIRTEL_MONEY","255683456789"));
        ArgumentCaptor<Payment> payment=ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertEquals(new BigDecimal("6000000.00"),payment.getValue().getAmount());
        assertEquals("FLUTTERWAVE",payment.getValue().getGateway());
        assertEquals(PaymentStatus.PROCESSING,response.status());
        assertNull(response.completedAt());
        assertEquals(12L,response.invoiceId());
        assertEquals("PENDING_PAYMENT",response.invoicePaymentStatus());
        assertEquals("SALES_CONFIRMED",response.orderStatus());
        verifyNoInteractions(invoiceService);
    }

    @Test void successfulFlutterwaveChargeSettlesInvoice() {
        when(invoices.findByIdAndOrderCustomerId(12L,7L)).thenReturn(Optional.of(invoice));
        when(invoices.findByIdForUpdate(12L)).thenReturn(Optional.of(invoice));
        when(payments.save(any())).thenAnswer(i -> { Payment p=i.getArgument(0); p.setId(1L); return p; });
        AtomicReference<String> reference = new AtomicReference<>();
        when(flutterwave.createMobileMoneyCharge(any(),any(),any(),any(),any(),any(),any())).thenAnswer(call -> { reference.set(call.getArgument(6)); return new FlutterwaveClient.ChargeResult("chg_test",reference.get(),null,invoice.getGrandTotal(),"TZS","succeeded",null,null,null,null); });
        when(flutterwave.retrieveCharge("chg_test")).thenAnswer(call -> new FlutterwaveClient.ChargeResult("chg_test",reference.get(),null,invoice.getGrandTotal(),"TZS","succeeded",null,null,null,null));

        PaymentResponse response=service.initiatePawaPayDeposit(12L,new PawaPayDepositRequest("AIRTEL_MONEY","255683456789"));

        assertEquals(PaymentStatus.COMPLETED,response.status());
        verify(invoiceService).confirmSuccessfulPayment(12L,"Flutterwave charge retrieval");
    }

    @Test void customerCannotInitiateAnotherCustomersInvoice() {
        when(invoices.findByIdAndOrderCustomerId(99L,7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.initiatePawaPayDeposit(99L,new PawaPayDepositRequest("AIRTEL_MONEY","255683456789")));
        verifyNoInteractions(invoiceService);
    }

    @Test void alreadyPaidInvoiceCannotBePaidAgain() {
        invoice.setPaymentStatus("PAID"); when(invoices.findByIdAndOrderCustomerId(12L,7L)).thenReturn(Optional.of(invoice)); when(invoices.findByIdForUpdate(12L)).thenReturn(Optional.of(invoice));
        assertThrows(BadRequestException.class,()->service.initiatePawaPayDeposit(12L,new PawaPayDepositRequest("AIRTEL_MONEY","255683456789")));
        verifyNoInteractions(invoiceService);
    }

    @Test void completedCallbackSettlesOnlyTheMatchingPaymentAndIsIdempotent() {
        Payment payment=Payment.builder().invoice(invoice).paymentReference("PWP-1").gateway("PAWAPAY").paymentMethod("AIRTEL_MONEY").gatewayTransactionId("f4401bd2-1568-4140-bf2d-eb77d2b2b639").amount(invoice.getGrandTotal()).currency("TZS").status(PaymentStatus.PROCESSING).build();
        when(payments.findByGatewayTransactionIdForUpdate(payment.getGatewayTransactionId())).thenReturn(Optional.of(payment));
        doAnswer(call->{invoice.setPaymentStatus("PAID");invoice.getOrder().setOrderStatus("PAYMENT_CONFIRMED");return null;}).when(invoiceService).confirmSuccessfulPayment(12L,"pawaPay callback");
        PawaPayDepositCallback callback=new PawaPayDepositCallback(payment.getGatewayTransactionId(),"COMPLETED",invoice.getGrandTotal(),"TZS","provider-ref",null);
        assertEquals(PaymentStatus.COMPLETED,service.processPawaPayDepositCallback(callback).status());
        assertEquals("PAID",invoice.getPaymentStatus()); verify(invoiceService).confirmSuccessfulPayment(12L,"pawaPay callback");
        service.processPawaPayDepositCallback(callback); verifyNoMoreInteractions(invoiceService);
    }

    @Test void failedCallbackDoesNotSettleInvoice() {
        Payment payment=Payment.builder().invoice(invoice).paymentReference("PWP-2").gateway("PAWAPAY").paymentMethod("HALOPESA").gatewayTransactionId("f4401bd2-1568-4140-bf2d-eb77d2b2b639").amount(invoice.getGrandTotal()).currency("TZS").status(PaymentStatus.PROCESSING).build();
        when(payments.findByGatewayTransactionIdForUpdate(payment.getGatewayTransactionId())).thenReturn(Optional.of(payment));
        PawaPayDepositCallback callback=new PawaPayDepositCallback(payment.getGatewayTransactionId(),"FAILED",invoice.getGrandTotal(),"TZS",null,new PawaPayDepositCallback.FailureReason("PAYMENT_NOT_APPROVED","Payment not approved"));
        assertEquals(PaymentStatus.FAILED,service.processPawaPayDepositCallback(callback).status());
        assertEquals("PENDING_PAYMENT",invoice.getPaymentStatus()); verifyNoInteractions(invoiceService);
    }

    @Test void typedFlutterwaveFailureIsSafeAndDoesNotSettleInvoice() {
        when(invoices.findByIdAndOrderCustomerId(12L,7L)).thenReturn(Optional.of(invoice));
        when(invoices.findByIdForUpdate(12L)).thenReturn(Optional.of(invoice));
        when(payments.save(any())).thenAnswer(i -> { Payment p=i.getArgument(0); p.setId(1L); return p; });
        when(flutterwave.createMobileMoneyCharge(any(),any(),any(),any(),any(),any(),any()))
                .thenThrow(new FlutterwaveException("charge creation", 400, "10400", "Currency not supported for TZ Mobile Money.", "trace-1", null));

        PaymentResponse response=service.initiatePawaPayDeposit(12L,new PawaPayDepositRequest("AIRTEL_MONEY","+255682328642"));

        assertEquals(PaymentStatus.FAILED,response.status());
        assertEquals("Flutterwave charge creation failed (400): Currency not supported for TZ Mobile Money.",response.failureReason());
        assertEquals("PENDING_PAYMENT",invoice.getPaymentStatus());
        verifyNoInteractions(invoiceService);
    }

    @Test void normalizesTanzanianPhoneWithoutDuplicatingCountryCode() {
        assertEquals("255682328642", PaymentServiceImpl.normalizeTanzanianPhone("+255682328642"));
        assertEquals("255682328642", PaymentServiceImpl.normalizeTanzanianPhone("0682328642"));
        assertThrows(BadRequestException.class, () -> PaymentServiceImpl.normalizeTanzanianPhone("+255255682328642"));
    }

}
