package com.securepay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SetTransactionPinRequest {

    @NotBlank(message = "Current account password is required")
    private String currentPassword;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "\\d{6}", message = "Transaction PIN must contain exactly 6 digits")
    private String transactionPin;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getTransactionPin() { return transactionPin; }
    public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }
}
