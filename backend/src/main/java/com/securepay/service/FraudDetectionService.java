package com.securepay.service;

import com.securepay.dto.FraudAlertDto;
import com.securepay.dto.FraudEvaluationResult;
import com.securepay.dto.FraudRuleDto;
import com.securepay.model.Transaction;
import com.securepay.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface FraudDetectionService {

    FraudEvaluationResult evaluateTransaction(User sender, User receiver, BigDecimal amount, String deviceId, String location);

    void recordFraudAlert(Transaction transaction, FraudEvaluationResult evaluationResult);

    List<FraudRuleDto> getAllRules();

    FraudRuleDto toggleRule(Long ruleId, boolean enabled);

    List<FraudAlertDto> getAllAlerts();

    FraudAlertDto updateAlertStatus(Long alertId, String status);

    FraudRuleDto createRule(FraudRuleDto dto);

    FraudRuleDto updateRule(Long id, FraudRuleDto dto);

    void deleteRule(Long id);
}
