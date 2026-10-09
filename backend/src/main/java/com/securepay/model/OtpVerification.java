package com.securepay.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "otp_verifications", indexes = {
        @Index(name = "idx_otp_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_otp_used", columnList = "used")
})
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OtpVerification() {
    }

    public OtpVerification(Long id, Transaction transaction, User user, String otpHash, Instant expiresAt,
                           Integer attempts, boolean used, Instant createdAt) {
        this.id = id;
        this.transaction = transaction;
        this.user = user;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.attempts = attempts != null ? attempts : 0;
        this.used = used;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static OtpVerificationBuilder builder() {
        return new OtpVerificationBuilder();
    }

    public static class OtpVerificationBuilder {
        private Long id;
        private Transaction transaction;
        private User user;
        private String otpHash;
        private Instant expiresAt;
        private Integer attempts = 0;
        private boolean used = false;
        private Instant createdAt;

        public OtpVerificationBuilder id(Long id) { this.id = id; return this; }
        public OtpVerificationBuilder transaction(Transaction transaction) { this.transaction = transaction; return this; }
        public OtpVerificationBuilder user(User user) { this.user = user; return this; }
        public OtpVerificationBuilder otpHash(String otpHash) { this.otpHash = otpHash; return this; }
        public OtpVerificationBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public OtpVerificationBuilder attempts(Integer attempts) { this.attempts = attempts; return this; }
        public OtpVerificationBuilder used(boolean used) { this.used = used; return this; }
        public OtpVerificationBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public OtpVerification build() {
            return new OtpVerification(id, transaction, user, otpHash, expiresAt, attempts, used, createdAt);
        }
    }
}
