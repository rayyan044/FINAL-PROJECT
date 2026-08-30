-- transport_charges is the single billable transport amount for every new order.
-- The routing columns are retained as an auditable snapshot of how a mapped delivery
-- selected its price. Do not update historical orders or invoices: their saved totals
-- and transport amounts remain authoritative.
COMMENT ON COLUMN fuel_orders.transport_charges IS 'Single final transport charge snapshot; distance rate replaces litre fallback for mapped deliveries.';
COMMENT ON COLUMN fuel_orders.distance_transport_price IS 'Deprecated non-billable routing-pricing field retained for backwards compatibility; new mapped orders store zero.';
