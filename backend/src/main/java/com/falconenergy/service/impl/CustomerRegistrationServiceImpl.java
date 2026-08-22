package com.falconenergy.service.impl;

import com.falconenergy.dto.CustomerRegistrationRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.entity.Customer;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.UserMapper;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.CustomerRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerRegistrationServiceImpl implements CustomerRegistrationService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(CustomerRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("An account already exists with this email address or username.");
        }
        // Existing-company linkage is deliberately staff-mediated: a public user
        // must not be able to claim a company's historical commercial records.
        if (customerRepository.existsByCompanyNameIgnoreCase(request.getCompanyName())
                || customerRepository.existsByEmailIgnoreCase(request.getCompanyEmail())) {
            throw new BadRequestException("This company already exists. Please ask Falcon staff to add you as an authorised company user.");
        }

        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .companyName(request.getCompanyName().trim())
                .contactPerson(request.getContactPerson().trim())
                .email(request.getCompanyEmail().trim().toLowerCase())
                .phone(request.getCompanyPhone())
                .address(request.getAddress())
                .tinNumber(request.getTinNumber())
                .status("ACTIVE")
                .build());

        Role customerRole = roleRepository.findByRoleName(UserRole.CUSTOMER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Customer role is not configured."));
        User user = User.builder()
                .username(request.getUsername().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .fullName(request.getFirstName().trim() + " " + request.getLastName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .roleEntity(customerRole)
                .customer(customer)
                .status(UserStatus.ACTIVE)
                .passwordChanged(true)
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }
}
