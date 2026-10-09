package com.securepay.dto;

import java.util.ArrayList;
import java.util.List;

public class FraudEvaluationResult {

    private Integer riskScore = 0;
    private String riskLevel = "LOW";
    private List<String> riskReasons = new ArrayList<>();
    private String decision = "ALLOW";
    private List<String> triggeredRules = new ArrayList<>();

    public FraudEvaluationResult() {
    }

    public FraudEvaluationResult(Integer riskScore, String riskLevel, List<String> riskReasons, String decision, List<String> triggeredRules) {
        this.riskScore = riskScore != null ? riskScore : 0;
        this.riskLevel = riskLevel != null ? riskLevel : "LOW";
        this.riskReasons = riskReasons != null ? riskReasons : new ArrayList<>();
        this.decision = decision != null ? decision : "ALLOW";
        this.triggeredRules = triggeredRules != null ? triggeredRules : new ArrayList<>();
    }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getRiskReasons() { return riskReasons; }
    public void setRiskReasons(List<String> riskReasons) { this.riskReasons = riskReasons; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }

    public static FraudEvaluationResultBuilder builder() {
        return new FraudEvaluationResultBuilder();
    }

    public static class FraudEvaluationResultBuilder {
        private Integer riskScore = 0;
        private String riskLevel = "LOW";
        private List<String> riskReasons = new ArrayList<>();
        private String decision = "ALLOW";
        private List<String> triggeredRules = new ArrayList<>();

        public FraudEvaluationResultBuilder riskScore(Integer riskScore) { this.riskScore = riskScore; return this; }
        public FraudEvaluationResultBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public FraudEvaluationResultBuilder riskReasons(List<String> riskReasons) { this.riskReasons = riskReasons; return this; }
        public FraudEvaluationResultBuilder decision(String decision) { this.decision = decision; return this; }
        public FraudEvaluationResultBuilder triggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; return this; }

        public FraudEvaluationResult build() {
            return new FraudEvaluationResult(riskScore, riskLevel, riskReasons, decision, triggeredRules);
        }
    }
}
