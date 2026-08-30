-- All pawaPay methods offered by this application are Tanzanian mobile-money
-- providers. Legacy payment attempts inherited USD from an order even though
-- their invoice amount and the gateway settlement currency are TZS.
UPDATE payments
SET currency = 'TZS'
WHERE gateway = 'PAWAPAY'
  AND currency = 'USD';
