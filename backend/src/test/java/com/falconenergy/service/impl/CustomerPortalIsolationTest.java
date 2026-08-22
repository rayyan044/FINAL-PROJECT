package com.falconenergy.service.impl;

import com.falconenergy.dto.CustomerPortalResponse;
import com.falconenergy.entity.*;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.*;
import com.falconenergy.service.FuelOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/** Verifies that portal resource IDs are resolved beneath the authenticated company. */
@ExtendWith(MockitoExtension.class)
class CustomerPortalIsolationTest {
    @Mock UserRepository users; @Mock CustomerRepository customers; @Mock FuelOrderRepository orders;
    @Mock InvoiceRepository invoices; @Mock PaymentReceiptRepository receipts; @Mock DeliveryRepository deliveries;
    @Mock DeliveryNoteRepository notes; @Mock FuelOrderService fuelOrderService;
    private CustomerPortalServiceImpl service;
    private Customer customerA;

    @BeforeEach void setup() {
        customerA = Customer.builder().customerCode("A").companyName("A Co").contactPerson("A").build(); customerA.setId(1L);
        Role role=Role.builder().roleName("CUSTOMER").build(); User user=User.builder().email("a@example.com").roleEntity(role).customer(customerA).build();
        when(users.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("a@example.com", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        service = new CustomerPortalServiceImpl(users, customers, orders, invoices, receipts, deliveries, notes, fuelOrderService);
    }
    @AfterEach void clean(){SecurityContextHolder.clearContext();}
    @Test void customerCannotReadAnotherCustomersOrder(){Customer b=customerB(); FuelOrder o=FuelOrder.builder().customer(b).build(); when(orders.findById(100L)).thenReturn(Optional.of(o)); assertThrows(ResourceNotFoundException.class,()->service.order(100L));}
    @Test void customerCannotReadAnotherCustomersInvoice(){when(invoices.findByIdAndOrderCustomerId(200L,1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class,()->service.invoice(200L));}
    @Test void customerCannotReadAnotherCustomersReceipt(){when(receipts.findByIdAndInvoiceOrderCustomerId(300L,1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class,()->service.receipt(300L));}
    @Test void customerCannotReadAnotherCustomersDelivery(){when(deliveries.findForCustomerById(400L,1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class,()->service.delivery(400L));}
    @Test void customerCannotReadAnotherCustomersDeliveryNote(){when(notes.findByIdAndCustomerId(500L,1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class,()->service.deliveryNote(500L));}
    @Test void timelineUsesCompletedDriverDeliveryOverEarlierOrderStatus(){
        FuelOrder order=FuelOrder.builder().customer(customerA).orderStatus("READY_FOR_DISPATCH").build(); order.setId(100L);
        LoadingOrder loadingOrder=LoadingOrder.builder().order(order).build();
        Delivery delivery=Delivery.builder().loadingOrder(loadingOrder).deliveryStatus(DeliveryStatus.COMPLETED).build();
        when(orders.findById(100L)).thenReturn(Optional.of(order));
        when(deliveries.findForCustomer(1L)).thenReturn(List.of(delivery));

        CustomerPortalResponse.Timeline timeline=service.timeline(100L);

        assertEquals("Completed", timeline.getCurrentStatus());
        assertEquals("COMPLETED", timeline.getSteps().stream().filter(CustomerPortalResponse.TimelineStep::isCurrent).findFirst().orElseThrow().getKey());
        assertTrue(timeline.getSteps().stream().filter(s -> "DELIVERED".equals(s.getKey()) || "COMPLETED".equals(s.getKey())).allMatch(CustomerPortalResponse.TimelineStep::isComplete));
    }
    @Test void timelineMarksDeliveredAsReachedBeforeFinalCompletion(){
        FuelOrder order=FuelOrder.builder().customer(customerA).orderStatus("READY_FOR_DISPATCH").build(); order.setId(101L);
        LoadingOrder loadingOrder=LoadingOrder.builder().order(order).build();
        Delivery delivery=Delivery.builder().loadingOrder(loadingOrder).deliveryStatus(DeliveryStatus.DELIVERED).build();
        when(orders.findById(101L)).thenReturn(Optional.of(order));
        when(deliveries.findForCustomer(1L)).thenReturn(List.of(delivery));

        CustomerPortalResponse.Timeline timeline=service.timeline(101L);

        assertEquals("Delivered", timeline.getCurrentStatus());
        assertTrue(timeline.getSteps().stream().filter(s -> "DELIVERED".equals(s.getKey())).findFirst().orElseThrow().isComplete());
        assertFalse(timeline.getSteps().stream().filter(s -> "COMPLETED".equals(s.getKey())).findFirst().orElseThrow().isCurrent());
    }
    private Customer customerB(){Customer b=Customer.builder().customerCode("B").companyName("B Co").contactPerson("B").build();b.setId(2L);return b;}
}
