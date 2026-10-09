package com.securepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Receiver username or email is required")
    private String receiverUsernameOrEmail;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal amount;

    @Pattern(regexp = "\\d{6}", message = "Transaction PIN must contain exactly 6 digits")
    private String transactionPin;

    private String description;
    private String deviceId;
    private String ipAddress;
    private String location;

    public TransferRequest() {
    }

    public TransferRequest(String receiverUsernameOrEmail, BigDecimal amount, String description) {
        this.receiverUsernameOrEmail = receiverUsernameOrEmail;
        this.amount = amount;
        this.description = description;
    }

    public TransferRequest(String receiverUsernameOrEmail, BigDecimal amount, String description,
                           String deviceId, String ipAddress, String location) {
        this.receiverUsernameOrEmail = receiverUsernameOrEmail;
        this.amount = amount;
        this.description = description;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.location = location;
    }

    public String getReceiverUsernameOrEmail() { return receiverUsernameOrEmail; }
    public void setReceiverUsernameOrEmail(String receiverUsernameOrEmail) { this.receiverUsernameOrEmail = receiverUsernameOrEmail; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionPin() { return transactionPin; }
    public void setTransactionPin(String transactionPin) { this.transactionPin = transactionPin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public static TransferRequestBuilder builder() {
        return new TransferRequestBuilder();
    }

    public static class TransferRequestBuilder {
        private String receiverUsernameOrEmail;
        private BigDecimal amount;
        private String transactionPin;
        private String description;
        private String deviceId;
        private String ipAddress;
        private String location;

        public TransferRequestBuilder receiverUsernameOrEmail(String receiverUsernameOrEmail) { this.receiverUsernameOrEmail = receiverUsernameOrEmail; return this; }
        public TransferRequestBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransferRequestBuilder transactionPin(String transactionPin) { this.transactionPin = transactionPin; return this; }
        public TransferRequestBuilder description(String description) { this.description = description; return this; }
        public TransferRequestBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public TransferRequestBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TransferRequestBuilder location(String location) { this.location = location; return this; }

        public TransferRequest build() {
            TransferRequest request = new TransferRequest(receiverUsernameOrEmail, amount, description, deviceId, ipAddress, location);
            request.setTransactionPin(transactionPin);
            return request;
        }
    }
}
