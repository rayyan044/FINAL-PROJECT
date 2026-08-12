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

    @Test
    void normalCustomerDisplaysRegisteredCompanyName() {
        Customer registeredCustomer = Customer.builder().companyName("Acme Logistics Ltd").customerCode("CUST-001").build();
        FuelOrder order = FuelOrder.builder().customer(registeredCustomer).driverName("John Doe").build();
        assertEquals("Acme Logistics Ltd", BuyerNameResolver.resolveName(order));
    }

    @Test
    void emergencyCustomerWithEnteredCompanyNameDisplaysDriverName() {
        Customer emergencyCustomer = Customer.builder().companyName("Customer Fuel Requests").customerCode("EMERGENCY").build();
        FuelOrder order = FuelOrder.builder().customer(emergencyCustomer).driverName("ABC Trading Company Ltd").build();
        assertEquals("ABC Trading Company Ltd", BuyerNameResolver.resolveName(order));
    }

    @Test
    void emergencyCustomerWithNullDriverNameFallsBackToCustomerName() {
        Customer emergencyCustomer = Customer.builder().companyName("Customer Fuel Requests").customerCode("EMERGENCY").build();
        FuelOrder order = FuelOrder.builder().customer(emergencyCustomer).driverName(null).build();
        assertEquals("Customer Fuel Requests", BuyerNameResolver.resolveName(order));
    }

    @Test
    void emergencyCustomerWithEmptyDriverNameFallsBackToCustomerName() {
        Customer emergencyCustomer = Customer.builder().companyName("Customer Fuel Requests").customerCode("EMERGENCY").build();
        FuelOrder order = FuelOrder.builder().customer(emergencyCustomer).driverName("").build();
        assertEquals("Customer Fuel Requests", BuyerNameResolver.resolveName(order));
    }

    @Test
    void emergencyCustomerWithWhitespaceDriverNameFallsBackToCustomerName() {
        Customer emergencyCustomer = Customer.builder().companyName("Customer Fuel Requests").customerCode("EMERGENCY").build();
        FuelOrder order = FuelOrder.builder().customer(emergencyCustomer).driverName("   ").build();
        assertEquals("Customer Fuel Requests", BuyerNameResolver.resolveName(order));
    }

    @Test
    void emergencyOrdersResolveIndependently() {
        Customer emergencyCustomer = Customer.builder().companyName("Customer Fuel Requests").customerCode("EMERGENCY").build();
        FuelOrder order1 = FuelOrder.builder().customer(emergencyCustomer).driverName("ABC Trading Company Ltd").build();
        FuelOrder order2 = FuelOrder.builder().customer(emergencyCustomer).driverName("XYZ Petroleum Ltd").build();
        assertEquals("ABC Trading Company Ltd", BuyerNameResolver.resolveName(order1));
        assertEquals("XYZ Petroleum Ltd", BuyerNameResolver.resolveName(order2));
    }
}
