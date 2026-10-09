package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.OtpSendRequest;
import com.securepay.dto.OtpVerifyRequest;
import com.securepay.dto.TransactionDto;
import com.securepay.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<TransactionDto>> sendOtp(@Valid @RequestBody OtpSendRequest sendRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        TransactionDto transactionDto = otpService.sendOtpForReferenceCode(username, sendRequest);

        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your registered email", transactionDto));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TransactionDto>> verifyOtp(@Valid @RequestBody OtpVerifyRequest verifyRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        TransactionDto result = otpService.verifyOtp(username, verifyRequest);

        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", result));
    }
}
