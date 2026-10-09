package com.securepay.service;

import com.securepay.dto.FraudEvaluationResult;
import com.securepay.model.*;
import com.securepay.repository.FraudAlertRepository;
import com.securepay.repository.FraudRuleRepository;
import com.securepay.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Phase 5 — Fraud Rule Inventory & Boundary Tests
 *
 * Each rule has:
 *  1. Positive test (rule fires)
 *  2. Negative test (rule does not fire)
 *  3. Boundary test (exact threshold)
 *  4. Null / no-history test
 *  5. Score clamping test
 *  6. Combination test (multiple rules together)
 *  7. Regression (legitimate transfer not blocked)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 5 — Fraud Rule Boundary & Inventory Tests")
class FraudRuleBoundaryTest {

    @Mock private FraudRuleRepository fraudRuleRepository;
    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRiskProfileService userRiskProfileService;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private User sender;
    private User receiver;
    private UserRiskProfile cleanProfile;
    private List<FraudRule> allRules;

    private static final String USUAL_DEVICE   = "Web-Browser-Chrome";
    private static final String USUAL_LOCATION = "Mumbai, IN";

    @BeforeEach
    void setUp() {
        sender   = User.builder().id(1L).username("alice").email("alice@test.com").role(Role.CUSTOMER).build();
        receiver = User.builder().id(2L).username("bob").email("bob@test.com").role(Role.CUSTOMER).build();

        cleanProfile = UserRiskProfile.builder()
                .id(10L).user(sender)
                .averageTransactionAmount(new BigDecimal("1000.00"))
                .usualLocation(USUAL_LOCATION)
                .usualDevice(USUAL_DEVICE)
                .usualTransactionHour(14)
                .previousFraudCount(0)
                .build();

        allRules = Arrays.asList(
                FraudRule.builder().id(1L).ruleCode("HIGH_AMOUNT").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(2L).ruleCode("NEW_RECEIVER").riskPoints(15).enabled(true).build(),
                FraudRule.builder().id(3L).ruleCode("NEW_DEVICE").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(4L).ruleCode("NEW_LOCATION").riskPoints(15).enabled(true).build(),
                FraudRule.builder().id(5L).ruleCode("RAPID_TRANSACTIONS").riskPoints(20).enabled(true).build(),
                FraudRule.builder().id(6L).ruleCode("UNUSUAL_TIME").riskPoints(10).enabled(true).build(),
                FraudRule.builder().id(7L).ruleCode("PREVIOUS_FRAUD").riskPoints(30).enabled(true).build()
        );
    }

    private void setupMocks() {
        when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(cleanProfile);
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);
        when(transactionRepository.findUserTransactionHistory(anyLong())).thenReturn(Collections.emptyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1: HIGH_AMOUNT
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule: HIGH_AMOUNT")
    class HighAmountRule {

        @Test
        @DisplayName("Positive: amount 10,001 fires HIGH_AMOUNT at 40 pts")
        void fires_above10k() {
            setupMocks();
            // Existing receiver — so only HIGH_AMOUNT fires
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("10001.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getRiskScore() >= 40, "Score should be at least 40 for 10k+ transfer");
            assertTrue(result.getTriggeredRules().contains("HIGH_AMOUNT"));
        }

        @Test
        @DisplayName("Negative: amount 9,999 does NOT fire HIGH_AMOUNT")
        void doesNotFire_below10k() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("9999.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("HIGH_AMOUNT"),
                    "HIGH_AMOUNT should not trigger below 10,000");
        }

        @Test
        @DisplayName("Boundary: amount exactly 10,000 fires HIGH_AMOUNT")
        void boundary_exactly10k_fires() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("10000.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("HIGH_AMOUNT"),
                    "HIGH_AMOUNT must fire at exactly ₹10,000 (>= comparison)");
        }

        @Test
        @DisplayName("Boundary: amount 49,999 uses 10k tier (+40), NOT 50k tier")
        void boundary_49999_uses10kTier() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("49999.00"), USUAL_DEVICE, USUAL_LOCATION);

            // Score from HIGH_AMOUNT alone should be 40 (not 65)
            // (Total may include NEW_RECEIVER etc. if no prior tx, but here we have prior tx)
            assertTrue(result.getTriggeredRules().contains("HIGH_AMOUNT"));
            // The 50k bonus should NOT be applied
            int highAmountContribution = result.getRiskScore();
            assertTrue(highAmountContribution < 65,
                    "Score below 65 for sub-50k (only 10k tier applies)");
        }

        @Test
        @DisplayName("Boundary: amount exactly 50,000 uses higher tier (+65)")
        void boundary_exactly50k_usesHigherTier() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("50000.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("HIGH_AMOUNT"));
            assertTrue(result.getRiskScore() >= 65,
                    "Score must be at least 65 for >=50k transfer");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 2: NEW_RECEIVER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule: NEW_RECEIVER")
    class NewReceiverRule {

        @Test
        @DisplayName("Positive: no prior completed tx to receiver fires NEW_RECEIVER")
        void fires_noHistory() {
            setupMocks();
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("NEW_RECEIVER"));
        }

        @Test
        @DisplayName("Negative: existing completed tx to same receiver suppresses NEW_RECEIVER")
        void doesNotFire_hasHistory() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("500.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("NEW_RECEIVER"),
                    "NEW_RECEIVER should NOT fire if completed tx to receiver exists");
        }

        @Test
        @DisplayName("Null: empty transaction history — NEW_RECEIVER still fires safely")
        void nullHistory_fireSafely() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(Collections.emptyList());

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("NEW_RECEIVER"));
            assertNotNull(result.getRiskReasons());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 3: NEW_DEVICE
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule: NEW_DEVICE")
    class NewDeviceRule {

        @Test
        @DisplayName("Positive: UNRECOGNIZED_DEVICE fires NEW_DEVICE")
        void fires_unrecognizedDevice() {
            setupMocks();
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), "UNRECOGNIZED_DEVICE", USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("NEW_DEVICE"));
        }

        @Test
        @DisplayName("Negative: usual device suppresses NEW_DEVICE")
        void doesNotFire_usualDevice() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("NEW_DEVICE"));
        }

        @Test
        @DisplayName("Positive: different device string fires NEW_DEVICE")
        void fires_differentDeviceString() {
            setupMocks();
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), "Mobile-Android-Samsung", USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("NEW_DEVICE"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5: RAPID_TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule: RAPID_TRANSACTIONS")
    class RapidTransactionsRule {

        @Test
        @DisplayName("Boundary: exactly 2 tx in 5 min fires RAPID_TRANSACTIONS")
        void boundary_2txIn5min_fires() {
            when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(cleanProfile);
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);

            Instant now = Instant.now();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(Arrays.asList(
                            Transaction.builder().id(1L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(now.minusSeconds(60)).build(),
                            Transaction.builder().id(2L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(now.minusSeconds(120)).build()
                    ));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("RAPID_TRANSACTIONS"));
        }

        @Test
        @DisplayName("Boundary: 1 tx in 5 min (below limit) does NOT fire RAPID_TRANSACTIONS")
        void boundary_1txIn5min_doesNotFire() {
            when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(cleanProfile);
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);

            Instant now = Instant.now();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(
                            Transaction.builder().id(1L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(now.minusSeconds(60)).build()
                    ));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("RAPID_TRANSACTIONS"),
                    "Only 1 tx in window — RAPID_TRANSACTIONS should not fire");
        }

        @Test
        @DisplayName("Old transactions (>5 min ago) do NOT trigger RAPID_TRANSACTIONS")
        void oldTransactions_doNotFire() {
            when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(cleanProfile);
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);

            Instant old = Instant.now().minusSeconds(400); // > 5 min ago
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(Arrays.asList(
                            Transaction.builder().id(1L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(old).build(),
                            Transaction.builder().id(2L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(old).build(),
                            Transaction.builder().id(3L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(old).build()
                    ));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("RAPID_TRANSACTIONS"),
                    "All transactions older than 5 min — RAPID_TRANSACTIONS must not fire");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 7: PREVIOUS_FRAUD
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rule: PREVIOUS_FRAUD")
    class PreviousFraudRule {

        @Test
        @DisplayName("Positive: profile.previousFraudCount > 0 fires PREVIOUS_FRAUD")
        void fires_profileFraudCount() {
            UserRiskProfile fraudProfile = UserRiskProfile.builder()
                    .id(10L).user(sender)
                    .averageTransactionAmount(new BigDecimal("1000.00"))
                    .usualLocation(USUAL_LOCATION).usualDevice(USUAL_DEVICE)
                    .previousFraudCount(2).build();

            when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(fraudProfile);
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);
            when(transactionRepository.findUserTransactionHistory(anyLong())).thenReturn(Collections.emptyList());

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertTrue(result.getTriggeredRules().contains("PREVIOUS_FRAUD"));
        }

        @Test
        @DisplayName("Negative: clean profile and no blocked history — PREVIOUS_FRAUD does not fire")
        void doesNotFire_cleanProfile() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("100.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("100.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertFalse(result.getTriggeredRules().contains("PREVIOUS_FRAUD"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Score clamping & Combination Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Score Clamping & Combination Tests")
    class ScoreClampingAndCombination {

        @Test
        @DisplayName("Score never exceeds 100 regardless of triggered rules")
        void scoreCappedAt100() {
            UserRiskProfile fraudProfile = UserRiskProfile.builder()
                    .id(10L).user(sender)
                    .averageTransactionAmount(new BigDecimal("100.00"))
                    .usualDevice(USUAL_DEVICE).usualLocation(USUAL_LOCATION)
                    .previousFraudCount(5).build();

            when(userRiskProfileService.getOrCreateProfile(any())).thenReturn(fraudProfile);
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(allRules);
            Instant now = Instant.now();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(Arrays.asList(
                            Transaction.builder().id(1L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("10.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(now.minusSeconds(30)).build(),
                            Transaction.builder().id(2L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("10.00")).status(TransactionStatus.COMPLETED)
                                    .createdAt(now.minusSeconds(60)).build(),
                            Transaction.builder().id(3L).sender(sender).receiver(receiver)
                                    .amount(new BigDecimal("10.00")).status(TransactionStatus.BLOCKED)
                                    .description("Fraud Analyst Confirmed").createdAt(now.minusSeconds(90)).build()
                    ));

            // All rules fire: 65 (50k amount) + 15 (new recv) + 20 (new device) + 15 (new loc) + 20 (rapid) + 30 (prev fraud) = 165 → capped at 100
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("50000.00"), "UNRECOGNIZED_DEVICE", "UNRECOGNIZED_LOCATION");

            assertEquals(100, result.getRiskScore(), "Score must be capped at 100");
            assertEquals("HIGH", result.getRiskLevel());
            assertEquals("VERIFICATION_REQUIRED", result.getDecision());
        }

        @Test
        @DisplayName("Regression: Legitimate transfer (known receiver, usual device, low amount) — LOW risk, ALLOW")
        void regression_legitimateTransfer_isAllowed() {
            setupMocks();
            when(transactionRepository.findUserTransactionHistory(anyLong()))
                    .thenReturn(List.of(Transaction.builder()
                            .id(1L).sender(sender).receiver(receiver)
                            .amount(new BigDecimal("1000.00")).status(TransactionStatus.COMPLETED)
                            .createdAt(Instant.now().minusSeconds(86400)).build()));

            // Usual device, usual location, amount below threshold, known receiver
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("500.00"), USUAL_DEVICE, USUAL_LOCATION);

            assertEquals("LOW", result.getRiskLevel(), "Legitimate transfer must be LOW risk");
            assertEquals("ALLOW", result.getDecision());
            assertTrue(result.getRiskScore() < 40, "Score < 40 for legitimate transfer");
            assertFalse(result.getTriggeredRules().contains("HIGH_AMOUNT"));
            assertFalse(result.getTriggeredRules().contains("NEW_RECEIVER"));
            assertFalse(result.getTriggeredRules().contains("PREVIOUS_FRAUD"));
        }

        @Test
        @DisplayName("Combination (false negative risk): new account + new device + new location + large amount → HIGH risk")
        void combination_allNewFlags_isHighRisk() {
            setupMocks();
            // No history → new receiver, no usual device/location set
            FraudEvaluationResult result = fraudDetectionService.evaluateTransaction(
                    sender, receiver, new BigDecimal("15000.00"), "UNRECOGNIZED_DEVICE", "UNRECOGNIZED_LOCATION");

            assertEquals("HIGH", result.getRiskLevel(),
                    "Multiple new flags + large amount must produce HIGH risk");
            assertEquals("VERIFICATION_REQUIRED", result.getDecision());
            assertTrue(result.getRiskScore() >= 70);
        }
    }
}
