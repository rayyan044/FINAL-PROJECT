package com.falconenergy.config;

import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class DatabaseBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final FuelProductRepository fuelProductRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public DatabaseBootstrap(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            FuelProductRepository fuelProductRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking if system requires initial administrator bootstrapping...");
        
        // Count users with ADMIN role
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();

        if (adminCount == 0) {
            log.info("No administrator found. Bootstrapping initial admin account...");

            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email("admin@falconenergy.com")
                    .username("admin")
                    .phone("+254700000000")
                    .password(passwordEncoder.encode("ChangeMe123!"))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .passwordChanged(false)
                    .build();

            User saved = userRepository.save(admin);
            log.info("Successfully bootstrapped initial admin account: admin@falconenergy.com / username: admin");

            // Log administrative action
            auditLogService.log(
                    "ADMIN_BOOTSTRAP",
                    "USER",
                    saved.getId(),
                    saved.getUsername(),
                    "Initial administrator account bootstrapped by system."
            );
        } else {
            log.info("System already has {} registered administrator(s). Bootstrapping skipped.", adminCount);
        }

        // Bootstrap default FINANCE user
        User finance = userRepository.findByUsername("finance")
                .or(() -> userRepository.findByEmail("finance@falconenergy.com"))
                .orElse(null);
        if (finance == null) {
            finance = User.builder()
                    .firstName("Sarah")
                    .lastName("Finance")
                    .email("finance@falconenergy.com")
                    .username("finance")
                    .phone("+254711111111")
                    .password(passwordEncoder.encode("ChangeMe123!"))
                    .role(UserRole.FINANCE)
                    .status(UserStatus.ACTIVE)
                    .passwordChanged(true)
                    .build();
            userRepository.save(finance);
            log.info("Successfully bootstrapped Finance account: finance@falconenergy.com / username: finance");
        }

        // Bootstrap default OPERATIONS user
        User operations = userRepository.findByUsername("operator")
                .or(() -> userRepository.findByEmail("operator@falconenergy.com"))
                .orElse(null);
        if (operations == null) {
            operations = User.builder()
                    .firstName("James")
                    .lastName("Operator")
                    .email("operator@falconenergy.com")
                    .username("operator")
                    .phone("+254722222222")
                    .password(passwordEncoder.encode("ChangeMe123!"))
                    .role(UserRole.OPERATIONS)
                    .status(UserStatus.ACTIVE)
                    .passwordChanged(true)
                    .build();
            userRepository.save(operations);
            log.info("Successfully bootstrapped Operations account: operator@falconenergy.com / username: operator");
        }

        // Bootstrap default SALES_OFFICER user
        User sales = userRepository.findByUsername("sales")
                .or(() -> userRepository.findByEmail("sales@falconenergy.com"))
                .orElse(null);
        if (sales == null) {
            sales = User.builder()
                    .firstName("Jane")
                    .lastName("Sales")
                    .email("sales@falconenergy.com")
                    .username("sales")
                    .phone("+254733333333")
                    .password(passwordEncoder.encode("ChangeMe123!"))
                    .role(UserRole.SALES_OFFICER)
                    .status(UserStatus.ACTIVE)
                    .passwordChanged(true)
                    .build();
            userRepository.save(sales);
            log.info("Successfully bootstrapped Sales account: sales@falconenergy.com / username: sales");
        }

        // Bootstrap default customer for emergency guest orders
        if (!customerRepository.existsByCustomerCode("EMERGENCY")) {
            log.info("Bootstrapping default EMERGENCY customer record...");
            Customer emergencyCust = Customer.builder()
                    .customerCode("EMERGENCY")
                    .companyName("Stranded Drivers (Emergency Requests)")
                    .contactPerson("Emergency Operator")
                    .phone("+254700000000")
                    .email("emergency@falconenergy.com")
                    .address("Falcon Energy HQ, Nairobi")
                    .status("ACTIVE")
                    .build();
            customerRepository.save(emergencyCust);
        }

        // Bootstrap default customer Acme Logistics Ltd
        if (!customerRepository.existsByCustomerCode("CUST-001")) {
            log.info("Bootstrapping default customer record: Acme Logistics Ltd...");
            Customer acmeCust = Customer.builder()
                    .customerCode("CUST-001")
                    .companyName("Acme Logistics Ltd")
                    .contactPerson("John Doe")
                    .phone("+254 700 000000")
                    .email("john@acmelogistics.com")
                    .address("Nairobi Industrial Area")
                    .tinNumber("TIN-ACME-001")
                    .status("ACTIVE")
                    .build();
            customerRepository.save(acmeCust);
        }

        // Bootstrap default fuel products if empty
        if (!fuelProductRepository.existsByProductNameIgnoreCaseUnfiltered("AGO (Diesel)")) {
            log.info("Bootstrapping default AGO (Diesel) fuel product...");
            FuelProduct ago = FuelProduct.builder()
                    .productName("AGO (Diesel)")
                    .fuelType("AGO")
                    .unitPrice(new BigDecimal("1.45"))
                    .density(new BigDecimal("0.840"))
                    .availableQuantity(new BigDecimal("100000.00"))
                    .status("ACTIVE")
                    .build();
            fuelProductRepository.save(ago);
        }

        if (!fuelProductRepository.existsByProductNameIgnoreCaseUnfiltered("PMS (Petrol)")) {
            log.info("Bootstrapping default PMS (Petrol) fuel product...");
            FuelProduct pms = FuelProduct.builder()
                    .productName("PMS (Petrol)")
                    .fuelType("PMS")
                    .unitPrice(new BigDecimal("1.55"))
                    .density(new BigDecimal("0.740"))
                    .availableQuantity(new BigDecimal("100000.00"))
                    .status("ACTIVE")
                    .build();
            fuelProductRepository.save(pms);
        }
    }
}
