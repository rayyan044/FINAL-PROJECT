package com.falconenergy.util;

import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.LoadingOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuyerNameResolverTest {

    @Test
    void resolvesCommercialBuyerFromTheInvoiceLinkedOrder() {
        Customer buyer = Customer.builder().companyName("Acme Commercial Fuels").build();
        FuelOrder invoicedOrder = FuelOrder.builder().customer(buyer).build();
        Invoice invoice = Invoice.builder().order(invoicedOrder).build();
        FuelOrder loadingOrderSource = FuelOrder.builder().customer(
                Customer.builder().companyName("Stranded Drivers (Emergency Requests)").build()).invoice(invoice).build();
        LoadingOrder loadingOrder = LoadingOrder.builder().order(loadingOrderSource).build();

        assertEquals("Acme Commercial Fuels", BuyerNameResolver.resolveName(loadingOrder));
    }

    @Test
    void retainsEmergencyBuyerForAnEmergencyInvoice() {
        Customer emergencyBuyer = Customer.builder().companyName("Stranded Drivers (Emergency Requests)").build();
        FuelOrder emergencyOrder = FuelOrder.builder().customer(emergencyBuyer).build();
        Invoice invoice = Invoice.builder().order(emergencyOrder).build();
        emergencyOrder.setInvoice(invoice);

        assertEquals("Stranded Drivers (Emergency Requests)", BuyerNameResolver.resolveName(emergencyOrder));
    }
}
