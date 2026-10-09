package com.securepay.dto;

import java.time.Instant;

public class FraudRuleDto {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private Integer riskPoints;
    private boolean enabled;
    private Instant updatedAt;

    public FraudRuleDto() {
    }

    public FraudRuleDto(Long id, String ruleCode, String ruleName, String description, Integer riskPoints, boolean enabled, Instant updatedAt) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.riskPoints = riskPoints;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

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

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Alias methods for compatibility with requested fields (name, points)
    public String getName() { return ruleName; }
    public void setName(String name) { this.ruleName = name; }
    public Integer getPoints() { return riskPoints; }
    public void setPoints(Integer points) { this.riskPoints = points; }


    public static FraudRuleDtoBuilder builder() {
        return new FraudRuleDtoBuilder();
    }

    public static class FraudRuleDtoBuilder {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String description;
        private Integer riskPoints;
        private boolean enabled;
        private Instant updatedAt;

        public FraudRuleDtoBuilder id(Long id) { this.id = id; return this; }
        public FraudRuleDtoBuilder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public FraudRuleDtoBuilder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public FraudRuleDtoBuilder description(String description) { this.description = description; return this; }
        public FraudRuleDtoBuilder riskPoints(Integer riskPoints) { this.riskPoints = riskPoints; return this; }
        public FraudRuleDtoBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public FraudRuleDtoBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public FraudRuleDto build() {
            return new FraudRuleDto(id, ruleCode, ruleName, description, riskPoints, enabled, updatedAt);
        }
    }
}
