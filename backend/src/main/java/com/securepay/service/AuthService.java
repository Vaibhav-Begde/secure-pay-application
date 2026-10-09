package com.securepay.service;

import com.securepay.dto.AuthResponse;
import com.securepay.dto.LoginRequest;
import com.securepay.dto.RegisterRequest;
import com.securepay.dto.SetTransactionPinRequest;
import com.securepay.dto.UserDto;

public interface AuthService {

    UserDto registerUser(RegisterRequest registerRequest);

    AuthResponse loginUser(LoginRequest loginRequest, String clientIp, String userAgent);

    UserDto getCurrentUser(String username);

    void setTransactionPin(String username, SetTransactionPinRequest request);
}
