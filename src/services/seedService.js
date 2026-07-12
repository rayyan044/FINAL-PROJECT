import { listProducts, createProduct } from "./productService";
import { listCustomers, createCustomer } from "./customerService";

export async function checkAndSeedDatabase() {
  try {
    // 1. Seed Products if empty
    const productsPage = await listProducts({ size: 1 });
    if (!productsPage.content || productsPage.content.length === 0) {
      console.log("Seeding default fuel products...");
      await createProduct({
        productName: "AGO (Diesel)",
        fuelType: "AGO",
        unitPrice: 1.45,
        density: 0.84,
        availableQuantity: 100000.0,
        status: "ACTIVE",
      });
      await createProduct({
        productName: "PMS (Petrol)",
        fuelType: "PMS",
        unitPrice: 1.55,
        density: 0.74,
        availableQuantity: 100000.0,
        status: "ACTIVE",
      });
    }

    // 2. Seed a Default Customer if empty
    const customersPage = await listCustomers({ size: 1 });
    if (!customersPage.content || customersPage.content.length === 0) {
      console.log("Seeding default customer...");
      await createCustomer({
        customerCode: "CUST-001",
        companyName: "Acme Logistics Ltd",
        contactPerson: "John Doe",
        phone: "+254 700 000000",
        email: "john@acmelogistics.com",
        address: "Nairobi Industrial Area",
        tinNumber: "TIN-ACME-001",
        status: "ACTIVE",
      });
    }
  } catch (e) {
    console.error("Seeding checks skipped (likely unauthorized or connection issue)", e);
  }
}
