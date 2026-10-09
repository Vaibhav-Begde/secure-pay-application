package com.securepay.service;

import com.securepay.dto.FraudEvaluationResult;
import com.securepay.model.FraudRule;
import com.securepay.model.Role;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.model.User;
import com.securepay.model.UserRiskProfile;
import com.securepay.repository.FraudAlertRepository;
import com.securepay.repository.FraudRuleRepository;
import com.securepay.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRiskProfileService userRiskProfileService;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private User sender;
    private User receiver;
    private UserRiskProfile defaultProfile;
    private List<FraudRule> rules;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(1L).username("senderUser").email("sender@example.com").role(Role.CUSTOMER).build();
        receiver = User.builder().id(2L).username("receiverUser").email("receiver@example.com").role(Role.CUSTOMER).build();

        defaultProfile = UserRiskProfile.builder()
                .id(10L)
                .user(sender)
                .averageTransactionAmount(new BigDecimal("250.00"))
                .usualLocation("New York, USA")
                .usualDevice("Web-Browser-Chrome")
                .usualTransactionHour(12)
                .previousFraudCount(0)
                .build();

        rules = Arrays.asList(
                FraudRule.builder().id(1L).ruleCode("HIGH_AMOUNT").ruleName("High Amount").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(2L).ruleCode("NEW_RECEIVER").ruleName("New Receiver").riskPoints(15).enabled(true).build(),
                FraudRule.builder().id(3L).ruleCode("NEW_DEVICE").ruleName("New Device").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(4L).ruleCode("NEW_LOCATION").ruleName("New Location").riskPoints(15).enabled(true).build(),
                FraudRule.builder().id(5L).ruleCode("RAPID_TRANSACTIONS").ruleName("Rapid Tx").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(6L).ruleCode("UNUSUAL_TIME").ruleName("Unusual Time").riskPoints(10).enabled(true).build(),
                FraudRule.builder().id(7L).ruleCode("PREVIOUS_FRAUD").ruleName("Previous Fraud").riskPoints(30).enabled(true).build()
        );
    }

    @Test
    void evaluateTransaction_LowRisk_AllowDecision() {
        when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(defaultProfile);
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(rules);
        when(transactionRepository.findUserTransactionHistory(anyLong())).thenReturn(Collections.emptyList());

        // Amount under threshold ($100), usual device & location => NEW_RECEIVER triggers (+15 points)
        FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(sender, receiver, new BigDecimal("100.00"), "Web-Browser-Chrome", "New York, USA");

        assertNotNull(result);
        assertEquals(15, result.getRiskScore());
        assertEquals("LOW", result.getRiskLevel());
        assertEquals("ALLOW", result.getDecision());
        assertTrue(result.getRiskReasons().contains("New receiver"));
    }

    @Test
    void evaluateTransaction_MediumRisk_OtpRequired() {
        when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(defaultProfile);
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(rules);
        when(transactionRepository.findUserTransactionHistory(anyLong())).thenReturn(Collections.emptyList());

        // Amount >= $1000 (+20), NEW_RECEIVER (+15), NEW_DEVICE (+20) => Total 55 (MEDIUM -> OTP_REQUIRED)
        FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(sender, receiver, new BigDecimal("1500.00"), "UNRECOGNIZED_DEVICE", "New York, USA");

        assertNotNull(result);
        assertEquals(55, result.getRiskScore());
        assertEquals("MEDIUM", result.getRiskLevel());
        assertEquals("OTP_REQUIRED", result.getDecision());
        assertTrue(result.getRiskReasons().contains("New device"));
        assertTrue(result.getRiskReasons().contains("Unusually high transaction amount (6.0x higher than average)"));
    }

    @Test
    void evaluateTransaction_HighRisk_VerificationRequired() {
        UserRiskProfile fraudHistoryProfile = UserRiskProfile.builder()
                .id(10L)
                .user(sender)
                .averageTransactionAmount(new BigDecimal("250.00"))
                .usualDevice("Web-Browser-Chrome")
                .usualLocation("New York, USA")
                .previousFraudCount(1)
                .build();

        when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(fraudHistoryProfile);
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(rules);
        when(transactionRepository.findUserTransactionHistory(anyLong())).thenReturn(Collections.emptyList());

        // Amount >= $1000 (+20), NEW_RECEIVER (+15), NEW_DEVICE (+20), NEW_LOCATION (+15), PREVIOUS_FRAUD (+30) => Total 100
        FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(sender, receiver, new BigDecimal("5000.00"), "UNRECOGNIZED_DEVICE", "UNRECOGNIZED_LOCATION");

        assertNotNull(result);
        assertEquals(100, result.getRiskScore());
        assertEquals("HIGH", result.getRiskLevel());
        assertEquals("VERIFICATION_REQUIRED", result.getDecision());
        assertTrue(result.getRiskReasons().contains("New receiver"));
        assertTrue(result.getRiskReasons().contains("New device"));
        assertTrue(result.getRiskReasons().contains("New location"));
        assertTrue(result.getRiskReasons().contains("Previous fraud history"));
    }

    @Test
    void evaluateTransaction_RiskScoreCappedAt100() {
        when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(defaultProfile);
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(rules);

        Instant now = Instant.now();
        List<Transaction> txHistory = Arrays.asList(
                Transaction.builder().id(101L).sender(sender).receiver(receiver).amount(new BigDecimal("10.00")).status(TransactionStatus.COMPLETED).createdAt(now).build(),
                Transaction.builder().id(102L).sender(sender).receiver(receiver).amount(new BigDecimal("10.00")).status(TransactionStatus.COMPLETED).createdAt(now).build(),
                Transaction.builder().id(103L).sender(sender).receiver(receiver).amount(new BigDecimal("10.00")).status(TransactionStatus.BLOCKED).description("Fraud Analyst Confirmed Fraud").createdAt(now).build()
        );
        when(transactionRepository.findUserTransactionHistory(1L)).thenReturn(txHistory);

        // Sum of all rules > 100 points => Capped at 100
        FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(sender, receiver, new BigDecimal("10000.00"), "UNRECOGNIZED_DEVICE", "UNRECOGNIZED_LOCATION");

        assertNotNull(result);
        assertEquals(100, result.getRiskScore());
        assertEquals("HIGH", result.getRiskLevel());
        assertEquals("VERIFICATION_REQUIRED", result.getDecision());
    }
}
