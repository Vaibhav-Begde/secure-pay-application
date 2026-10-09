package com.securepay.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fraud_rules", uniqueConstraints = {
        @UniqueConstraint(columnNames = "rule_code")
})
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(length = 255)
    private String description;

    @Column(name = "risk_points", nullable = false)
    private Integer riskPoints;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FraudRule() {
    }

    public FraudRule(Long id, String ruleCode, String ruleName, String description, Integer riskPoints, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.riskPoints = riskPoints != null ? riskPoints : 0;
        this.enabled = enabled;
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

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRiskPoints() { return riskPoints; }
    public void setRiskPoints(Integer riskPoints) { this.riskPoints = riskPoints; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static FraudRuleBuilder builder() {
        return new FraudRuleBuilder();
    }

    public static class FraudRuleBuilder {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String description;
        private Integer riskPoints = 0;
        private boolean enabled = true;
        private Instant createdAt;
        private Instant updatedAt;

        public FraudRuleBuilder id(Long id) { this.id = id; return this; }
        public FraudRuleBuilder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public FraudRuleBuilder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public FraudRuleBuilder description(String description) { this.description = description; return this; }
        public FraudRuleBuilder riskPoints(Integer riskPoints) { this.riskPoints = riskPoints; return this; }
        public FraudRuleBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public FraudRuleBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public FraudRuleBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public FraudRule build() {
            return new FraudRule(id, ruleCode, ruleName, description, riskPoints, enabled, createdAt, updatedAt);
        }
    }
}
