package com.falconenergy.service.impl;

import com.falconenergy.entity.*;
import com.falconenergy.mapper.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;

class InvoicePaymentSettlementTest {
    @Test void simulatedCustomerPaymentSettlesInvoiceProgressesOrderAndGeneratesReceipt() {
        InvoiceRepository invoices=mock(InvoiceRepository.class); FuelOrderRepository orders=mock(FuelOrderRepository.class);
        PaymentReceiptService receipts=mock(PaymentReceiptService.class); AuditLogService audit=mock(AuditLogService.class);
        InvoiceMapper mapper=mock(InvoiceMapper.class);
        InvoiceServiceImpl service=new InvoiceServiceImpl(invoices,mapper,audit,mock(CompanySettingsRepository.class),mock(CompanySettingsMapper.class),mock(PaymentAccountRepository.class),orders,mock(FuelProductRepository.class),mock(FuelProductMapper.class),mock(SystemSettingService.class),mock(OrderTruckAllocationRepository.class),receipts);
        Customer customer=Customer.builder().customerCode("C-1").companyName("Customer").build();
        FuelOrder order=FuelOrder.builder().customer(customer).orderStatus("SALES_CONFIRMED").build(); order.setId(8L);
        Invoice invoice=Invoice.builder().invoiceNumber("INV-1").order(order).paymentStatus("PENDING_PAYMENT").grandTotal(BigDecimal.TEN).build(); invoice.setId(5L);
        when(invoices.findByIdForUpdate(5L)).thenReturn(Optional.of(invoice)); when(invoices.save(any())).thenAnswer(i->i.getArgument(0));
        service.confirmSuccessfulPayment(5L,"Customer Payment Simulation");
        verify(receipts,times(1)).generateForPaidInvoice(invoice); verify(orders).save(order);
        org.junit.jupiter.api.Assertions.assertEquals("PAID", invoice.getPaymentStatus());
        org.junit.jupiter.api.Assertions.assertEquals("PAYMENT_CONFIRMED", order.getOrderStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(invoice.getFinanceApprovedAt());
        service.confirmSuccessfulPayment(5L,"Customer Payment Simulation");
        verify(receipts,times(1)).generateForPaidInvoice(invoice);
    }
}
