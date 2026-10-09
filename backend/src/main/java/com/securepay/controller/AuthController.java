package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.AuthResponse;
import com.securepay.dto.LoginRequest;
import com.securepay.dto.RegisterRequest;
import com.securepay.dto.SetTransactionPinRequest;
import com.securepay.dto.UserDto;
import com.securepay.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDto registeredUser = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("User authenticated successfully", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        String username = authentication.getName();
        UserDto currentUser = authService.getCurrentUser(username);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", currentUser));
    }

    @PostMapping("/transaction-pin")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(@Valid @RequestBody SetTransactionPinRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        authService.setTransactionPin(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Transaction PIN saved", null));
    }
}
