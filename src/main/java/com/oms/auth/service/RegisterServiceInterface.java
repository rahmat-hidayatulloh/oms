package com.oms.auth.service;

import com.oms.auth.dto.RegisterRequest;
import com.oms.auth.dto.RegisterResponse;

/**
 * Interface for user registration business operations.
 * Controllers depend on this abstraction (DIP) rather than the concrete implementation.
 */
public interface RegisterServiceInterface {

    RegisterResponse register(RegisterRequest request);
}
