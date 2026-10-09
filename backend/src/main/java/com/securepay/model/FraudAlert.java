package com.securepay.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_alert_user_id", columnList = "user_id"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_risk_level", columnList = "risk_level")
})
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "risk_reasons", columnDefinition = "TEXT")
    private String riskReasons;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FraudAlert() {
    }

    public FraudAlert(Long id, Transaction transaction, User user, Integer riskScore, String riskLevel,
                      String riskReasons, String decision, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.transaction = transaction;
        this.user = user;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskReasons = riskReasons;
        this.decision = decision;
        this.status = status != null ? status : "OPEN";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static FraudAlertBuilder builder() {
        return new FraudAlertBuilder();
    }

    public static class FraudAlertBuilder {
        private Long id;
        private Transaction transaction;
        private User user;
        private Integer riskScore;
        private String riskLevel;
        private String riskReasons;
        private String decision;
        private String status = "OPEN";
        private Instant createdAt;
        private Instant updatedAt;

        public FraudAlertBuilder id(Long id) { this.id = id; return this; }
        public FraudAlertBuilder transaction(Transaction transaction) { this.transaction = transaction; return this; }
        public FraudAlertBuilder user(User user) { this.user = user; return this; }
        public FraudAlertBuilder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public FraudAlertBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public FraudAlertBuilder riskReasons(String riskReasons) { this.riskReasons = riskReasons; return this; }
        public FraudAlertBuilder decision(String decision) { this.decision = decision; return this; }
        public FraudAlertBuilder status(String status) { this.status = status; return this; }
        public FraudAlertBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public FraudAlertBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public FraudAlert build() {
            return new FraudAlert(id, transaction, user, riskScore, riskLevel, riskReasons, decision, status, createdAt, updatedAt);
        }
    }
}
