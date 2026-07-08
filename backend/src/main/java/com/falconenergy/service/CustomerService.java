package com.falconenergy.service;

import com.falconenergy.dto.CustomerRequest;
import com.falconenergy.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse getCustomerById(Long id);
    CustomerResponse updateCustomer(Long id, CustomerRequest request);
    void deleteCustomer(Long id);
    Page<CustomerResponse> getAllCustomers(String search, String status, Pageable pageable);
}
