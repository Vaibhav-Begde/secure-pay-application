package com.securepay.dto;

import com.securepay.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionDto {

    private Long id;
    private String referenceCode;
    private String senderUsername;
    private String receiverUsername;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private String deviceId;
    private String ipAddress;
    private String location;
    private Instant transactionTime;
    private Integer riskScore;
    private String riskLevel;
    private Instant createdAt;

    public TransactionDto() {
    }

    public TransactionDto(Long id, String referenceCode, String senderUsername, String receiverUsername,
                          BigDecimal amount, TransactionStatus status, String description,
                          String deviceId, String ipAddress, String location, Instant transactionTime,
                          Integer riskScore, String riskLevel, Instant createdAt) {
        this.id = id;
        this.referenceCode = referenceCode;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.location = location;
        this.transactionTime = transactionTime;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Instant getTransactionTime() { return transactionTime; }
    public void setTransactionTime(Instant transactionTime) { this.transactionTime = transactionTime; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static TransactionDtoBuilder builder() {
        return new TransactionDtoBuilder();
    }

    public static class TransactionDtoBuilder {
        private Long id;
        private String referenceCode;
        private String senderUsername;
        private String receiverUsername;
        private BigDecimal amount;
        private TransactionStatus status;
        private String description;
        private String deviceId;
        private String ipAddress;
        private String location;
        private Instant transactionTime;
        private Integer riskScore;
        private String riskLevel;
        private Instant createdAt;

        public TransactionDtoBuilder id(Long id) { this.id = id; return this; }
        public TransactionDtoBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public TransactionDtoBuilder senderUsername(String senderUsername) { this.senderUsername = senderUsername; return this; }
        public TransactionDtoBuilder receiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; return this; }
        public TransactionDtoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransactionDtoBuilder status(TransactionStatus status) { this.status = status; return this; }
        public TransactionDtoBuilder description(String description) { this.description = description; return this; }
        public TransactionDtoBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public TransactionDtoBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TransactionDtoBuilder location(String location) { this.location = location; return this; }
        public TransactionDtoBuilder transactionTime(Instant transactionTime) { this.transactionTime = transactionTime; return this; }
        public TransactionDtoBuilder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public TransactionDtoBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public TransactionDtoBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TransactionDto build() {
            return new TransactionDto(id, referenceCode, senderUsername, receiverUsername, amount, status, description, deviceId, ipAddress, location, transactionTime, riskScore, riskLevel, createdAt);
        }
    }
}
