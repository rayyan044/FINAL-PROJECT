package com.falconenergy.service;

import com.falconenergy.dto.CustomerRegistrationRequest;
import com.falconenergy.dto.UserResponse;

public interface CustomerRegistrationService {
    UserResponse register(CustomerRegistrationRequest request);
}
