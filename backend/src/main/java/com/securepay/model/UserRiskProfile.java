package com.securepay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_risk_profiles")
public class UserRiskProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "average_transaction_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageTransactionAmount = BigDecimal.ZERO;

    @Column(name = "total_transaction_count", nullable = false)
    private Long totalTransactionCount = 0L;

    @Column(name = "usual_location", length = 100)
    private String usualLocation;

    @Column(name = "usual_device", length = 100)
    private String usualDevice;

    @Column(name = "usual_transaction_hour")
    private Integer usualTransactionHour = 12;

    @Column(name = "previous_fraud_count", nullable = false)
    private Integer previousFraudCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserRiskProfile() {
    }

    public UserRiskProfile(Long id, User user, BigDecimal averageTransactionAmount, Long totalTransactionCount,
                           String usualLocation, String usualDevice, Integer usualTransactionHour,
                           Integer previousFraudCount, Instant updatedAt) {
        this.id = id;
        this.user = user;
        this.averageTransactionAmount = averageTransactionAmount != null ? averageTransactionAmount : BigDecimal.ZERO;
        this.totalTransactionCount = totalTransactionCount != null ? totalTransactionCount : 0L;
        this.usualLocation = usualLocation;
        this.usualDevice = usualDevice;
        this.usualTransactionHour = usualTransactionHour != null ? usualTransactionHour : 12;
        this.previousFraudCount = previousFraudCount != null ? previousFraudCount : 0;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getAverageTransactionAmount() { return averageTransactionAmount; }
    public void setAverageTransactionAmount(BigDecimal averageTransactionAmount) { this.averageTransactionAmount = averageTransactionAmount; }

    public Long getTotalTransactionCount() { return totalTransactionCount; }
    public void setTotalTransactionCount(Long totalTransactionCount) { this.totalTransactionCount = totalTransactionCount; }

    public String getUsualLocation() { return usualLocation; }
    public void setUsualLocation(String usualLocation) { this.usualLocation = usualLocation; }

    public String getUsualDevice() { return usualDevice; }
    public void setUsualDevice(String usualDevice) { this.usualDevice = usualDevice; }

    public Integer getUsualTransactionHour() { return usualTransactionHour; }
    public void setUsualTransactionHour(Integer usualTransactionHour) { this.usualTransactionHour = usualTransactionHour; }

    public Integer getPreviousFraudCount() { return previousFraudCount; }
    public void setPreviousFraudCount(Integer previousFraudCount) { this.previousFraudCount = previousFraudCount; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static UserRiskProfileBuilder builder() {
        return new UserRiskProfileBuilder();
    }

    public static class UserRiskProfileBuilder {
        private Long id;
        private User user;
        private BigDecimal averageTransactionAmount = BigDecimal.ZERO;
        private Long totalTransactionCount = 0L;
        private String usualLocation;
        private String usualDevice;
        private Integer usualTransactionHour = 12;
        private Integer previousFraudCount = 0;
        private Instant updatedAt;

        public UserRiskProfileBuilder id(Long id) { this.id = id; return this; }
        public UserRiskProfileBuilder user(User user) { this.user = user; return this; }
        public UserRiskProfileBuilder averageTransactionAmount(BigDecimal averageTransactionAmount) { this.averageTransactionAmount = averageTransactionAmount; return this; }
        public UserRiskProfileBuilder totalTransactionCount(Long totalTransactionCount) { this.totalTransactionCount = totalTransactionCount; return this; }
        public UserRiskProfileBuilder usualLocation(String usualLocation) { this.usualLocation = usualLocation; return this; }
        public UserRiskProfileBuilder usualDevice(String usualDevice) { this.usualDevice = usualDevice; return this; }
        public UserRiskProfileBuilder usualTransactionHour(Integer usualTransactionHour) { this.usualTransactionHour = usualTransactionHour; return this; }
        public UserRiskProfileBuilder previousFraudCount(Integer previousFraudCount) { this.previousFraudCount = previousFraudCount; return this; }
        public UserRiskProfileBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserRiskProfile build() {
            return new UserRiskProfile(id, user, averageTransactionAmount, totalTransactionCount, usualLocation, usualDevice, usualTransactionHour, previousFraudCount, updatedAt);
        }
    }
}
