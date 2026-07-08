package com.falconenergy.service.impl;

import com.falconenergy.dto.CustomerRequest;
import com.falconenergy.dto.CustomerResponse;
import com.falconenergy.entity.Customer;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.CustomerMapper;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with code: {}", request.getCustomerCode());
        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new DuplicateResourceException("Customer code already exists: " + request.getCustomerCode());
        }
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer email already exists: " + request.getEmail());
        }
        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (!customer.getCustomerCode().equals(request.getCustomerCode()) &&
                customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new DuplicateResourceException("Customer code already exists: " + request.getCustomerCode());
        }
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail()) &&
                customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer email already exists: " + request.getEmail());
        }

        customerMapper.updateEntityFromRequest(request, customer);
        Customer updated = customerRepository.save(customer);
        return customerMapper.toResponse(updated);
    }

    @Override
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String search, String status, Pageable pageable) {
        Specification<Customer> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("customerCode")), wildcard),
                    cb.like(cb.lower(root.get("companyName")), wildcard),
                    cb.like(cb.lower(root.get("contactPerson")), wildcard),
                    cb.like(cb.lower(root.get("email")), wildcard)
            ));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }

        return customerRepository.findAll(spec, pageable).map(customerMapper::toResponse);
    }
}
