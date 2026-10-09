package com.securepay.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class FraudAlertDto {

    private Long id;
    private Long transactionId;
    private String referenceCode;
    private String senderUsername;
    private String receiverUsername;
    private BigDecimal amount;
    private Integer riskScore;
    private String riskLevel;
    private String riskReasons;
    private String decision;
    private String status;
    private Instant createdAt;

    public FraudAlertDto() {
    }

    public FraudAlertDto(Long id, Long transactionId, String referenceCode, String senderUsername, String receiverUsername,
                         BigDecimal amount, Integer riskScore, String riskLevel, String riskReasons,
                         String decision, String status, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.referenceCode = referenceCode;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.amount = amount;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskReasons = riskReasons;
        this.decision = decision;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRiskReasons() { return riskReasons; }
    public void setRiskReasons(String riskReasons) { this.riskReasons = riskReasons; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static FraudAlertDtoBuilder builder() {
        return new FraudAlertDtoBuilder();
    }

    public static class FraudAlertDtoBuilder {
        private Long id;
        private Long transactionId;
        private String referenceCode;
        private String senderUsername;
        private String receiverUsername;
        private BigDecimal amount;
        private Integer riskScore;
        private String riskLevel;
        private String riskReasons;
        private String decision;
        private String status;
        private Instant createdAt;

        public FraudAlertDtoBuilder id(Long id) { this.id = id; return this; }
        public FraudAlertDtoBuilder transactionId(Long transactionId) { this.transactionId = transactionId; return this; }
        public FraudAlertDtoBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public FraudAlertDtoBuilder senderUsername(String senderUsername) { this.senderUsername = senderUsername; return this; }
        public FraudAlertDtoBuilder receiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; return this; }
        public FraudAlertDtoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public FraudAlertDtoBuilder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public FraudAlertDtoBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public FraudAlertDtoBuilder riskReasons(String riskReasons) { this.riskReasons = riskReasons; return this; }
        public FraudAlertDtoBuilder decision(String decision) { this.decision = decision; return this; }
        public FraudAlertDtoBuilder status(String status) { this.status = status; return this; }
        public FraudAlertDtoBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public FraudAlertDto build() {
            return new FraudAlertDto(id, transactionId, referenceCode, senderUsername, receiverUsername, amount, riskScore, riskLevel, riskReasons, decision, status, createdAt);
        }
    }
}
