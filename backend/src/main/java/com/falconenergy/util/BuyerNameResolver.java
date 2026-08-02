package com.falconenergy.util;

import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Invoice;
import com.falconenergy.entity.LoadingOrder;

/**
 * Resolves the buyer for operational documents from the sales invoice.  The
 * order customer is retained as a legacy fallback for records created before
 * invoices were mandatory for loading orders.
 */
public final class BuyerNameResolver {

    private BuyerNameResolver() {
    }

    public static Customer resolveCustomer(FuelOrder order) {
        if (order == null) {
            return null;
        }
        Invoice invoice = order.getInvoice();
        if (invoice != null && invoice.getOrder() != null && invoice.getOrder().getCustomer() != null) {
            return invoice.getOrder().getCustomer();
        }
        return order.getCustomer();
    }

    public static String resolveName(LoadingOrder loadingOrder) {
        return loadingOrder == null ? null : resolveName(loadingOrder.getOrder());
    }

    public static String resolveName(FuelOrder order) {
        Customer customer = resolveCustomer(order);
        return customer == null ? null : customer.getCompanyName();
    }
}
