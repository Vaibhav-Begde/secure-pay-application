package com.securepay.service;

import java.math.BigDecimal;

public interface EmailService {

    /**
     * Send Step-Up OTP Verification Email to user's registered email
     *
     * @param toEmail User's recipient email address
     * @param username User's username
     * @param otpCode 6-digit OTP code
     * @param referenceCode Transaction reference code
     * @param amount Transaction transfer amount
     */
    void sendOtpEmail(String toEmail, String username, String otpCode, String referenceCode, BigDecimal amount);
}
