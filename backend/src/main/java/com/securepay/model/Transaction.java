package com.securepay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_code", nullable = false, unique = true, length = 50)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @Column(name = "transaction_time")
    private Instant transactionTime;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(name = "risk_level", length = 20)
    private String riskLevel = "LOW";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Transaction() {
    }

    public Transaction(Long id, String referenceCode, User sender, User receiver, BigDecimal amount,
                       TransactionStatus status, String description, String deviceId, String ipAddress,
                       String location, Instant transactionTime, Integer riskScore, String riskLevel, Instant createdAt) {
        this.id = id;
        this.referenceCode = referenceCode != null ? referenceCode : "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.location = location;
        this.transactionTime = transactionTime != null ? transactionTime : Instant.now();
        this.riskScore = riskScore != null ? riskScore : 0;
        this.riskLevel = riskLevel != null ? riskLevel : "LOW";
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.referenceCode == null) {
            this.referenceCode = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (this.transactionTime == null) {
            this.transactionTime = Instant.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

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

    // Builder
    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private Long id;
        private String referenceCode;
        private User sender;
        private User receiver;
        private BigDecimal amount;
        private TransactionStatus status;
        private String description;
        private String deviceId;
        private String ipAddress;
        private String location;
        private Instant transactionTime;
        private Integer riskScore = 0;
        private String riskLevel = "LOW";
        private Instant createdAt;

        public TransactionBuilder id(Long id) { this.id = id; return this; }
        public TransactionBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public TransactionBuilder sender(User sender) { this.sender = sender; return this; }
        public TransactionBuilder receiver(User receiver) { this.receiver = receiver; return this; }
        public TransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransactionBuilder status(TransactionStatus status) { this.status = status; return this; }
        public TransactionBuilder description(String description) { this.description = description; return this; }
        public TransactionBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public TransactionBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TransactionBuilder location(String location) { this.location = location; return this; }
        public TransactionBuilder transactionTime(Instant transactionTime) { this.transactionTime = transactionTime; return this; }
        public TransactionBuilder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public TransactionBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public TransactionBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Transaction build() {
            return new Transaction(id, referenceCode, sender, receiver, amount, status, description, deviceId, ipAddress, location, transactionTime, riskScore, riskLevel, createdAt);
        }
    }
}
