package com.securepay.service;

import com.securepay.dto.FraudAlertDto;
import com.securepay.dto.FraudEvaluationResult;
import com.securepay.dto.FraudRuleDto;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.model.*;
import com.securepay.repository.FraudAlertRepository;
import com.securepay.repository.FraudRuleRepository;
import com.securepay.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);
    private static final BigDecimal HIGH_AMOUNT_STATIC_THRESHOLD = new BigDecimal("10000.00");

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final UserRiskProfileService userRiskProfileService;

    @Autowired
    public FraudDetectionServiceImpl(FraudRuleRepository fraudRuleRepository,
            FraudAlertRepository fraudAlertRepository,
            TransactionRepository transactionRepository,
            UserRiskProfileService userRiskProfileService) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.userRiskProfileService = userRiskProfileService;
    }

    @Override
    @Transactional(readOnly = true)
    public FraudEvaluationResult evaluateTransaction(User sender, User receiver, BigDecimal amount, String deviceId,
            String location) {
        List<FraudRule> activeRules = fraudRuleRepository.findByEnabledTrue();
        Map<String, FraudRule> ruleMap = activeRules.stream()
                .collect(Collectors.toMap(FraudRule::getRuleCode, r -> r, (r1, r2) -> r1));

        UserRiskProfile profile = userRiskProfileService.getOrCreateProfile(sender);

        int totalRiskScore = 0;
        List<String> riskReasons = new ArrayList<>();
        List<String> triggeredRules = new ArrayList<>();

        // 1. HIGH_AMOUNT / Amount Deviation Rule
        if (ruleMap.containsKey("HIGH_AMOUNT") && amount != null) {
            BigDecimal avgAmount = profile.getAverageTransactionAmount();
            boolean isHighAmount = amount.compareTo(HIGH_AMOUNT_STATIC_THRESHOLD) >= 0;
            boolean isDeviatedAmount = avgAmount != null && avgAmount.compareTo(BigDecimal.ZERO) > 0 &&
                    amount.compareTo(avgAmount.multiply(new BigDecimal("3.0"))) >= 0;

            if (isHighAmount || isDeviatedAmount) {
                FraudRule rule = ruleMap.get("HIGH_AMOUNT");
                int points = rule.getRiskPoints();

                if (amount.compareTo(new BigDecimal("50000.00")) >= 0) {
                    points += 45; // High threshold -> 65 points base
                    riskReasons.add("High transfer amount exceeds ₹50,000.00 safety threshold");
                } else if (amount.compareTo(new BigDecimal("10000.00")) >= 0) {
                    points += 20; // Medium threshold -> 40 points base
                    riskReasons.add("Transfer amount exceeds ₹10,000.00 tier threshold");
                }

                totalRiskScore += points;
                triggeredRules.add(rule.getRuleCode());

                if (isDeviatedAmount) {
                    BigDecimal ratio = amount.divide(avgAmount, 1, RoundingMode.HALF_UP);
                    riskReasons.add("Unusually high transaction amount (" + ratio + "x higher than average)");
                } else if (amount.compareTo(new BigDecimal("10000.00")) < 0) {
                    riskReasons.add("Unusually high transaction amount");
                }
            }
        }

        // Fetch sender transaction history for rule evaluations
        List<Transaction> senderTxHistory = transactionRepository.findUserTransactionHistory(sender.getId());

        // 2. NEW_RECEIVER Rule
        if (ruleMap.containsKey("NEW_RECEIVER")) {
            boolean hasTransferredToReceiverBefore = senderTxHistory.stream()
                    .anyMatch(tx -> tx.getSender().getId().equals(sender.getId()) &&
                            tx.getReceiver().getId().equals(receiver.getId()) &&
                            tx.getStatus() == TransactionStatus.COMPLETED);

            if (!hasTransferredToReceiverBefore) {
                FraudRule rule = ruleMap.get("NEW_RECEIVER");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("New receiver");
            }
        }

        // 3. NEW_DEVICE Rule
        if (ruleMap.containsKey("NEW_DEVICE")) {
            String usualDevice = profile.getUsualDevice();
            boolean isNewDevice = (deviceId != null && !deviceId.isBlank() && usualDevice != null
                    && !deviceId.equalsIgnoreCase(usualDevice)) ||
                    (deviceId != null && deviceId.equalsIgnoreCase("UNRECOGNIZED_DEVICE"));

            if (isNewDevice) {
                FraudRule rule = ruleMap.get("NEW_DEVICE");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("New device");
            }
        }

        // 4. NEW_LOCATION Rule
        if (ruleMap.containsKey("NEW_LOCATION")) {
            String usualLocation = profile.getUsualLocation();
            boolean isNewLocation = (location != null && !location.isBlank() && usualLocation != null
                    && !location.equalsIgnoreCase(usualLocation)) ||
                    (location != null && location.equalsIgnoreCase("UNRECOGNIZED_LOCATION"));

            if (isNewLocation) {
                FraudRule rule = ruleMap.get("NEW_LOCATION");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("New location");
            }
        }

        // 5. RAPID_TRANSACTIONS Rule
        if (ruleMap.containsKey("RAPID_TRANSACTIONS")) {
            Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            long recentTxCount = senderTxHistory.stream()
                    .filter(tx -> tx.getSender().getId().equals(sender.getId())
                            && tx.getCreatedAt().isAfter(fiveMinutesAgo))
                    .count();

            if (recentTxCount >= 2) {
                FraudRule rule = ruleMap.get("RAPID_TRANSACTIONS");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("Rapid transaction velocity");
            }
        }

        // 6. UNUSUAL_TIME Rule
        if (ruleMap.containsKey("UNUSUAL_TIME")) {
            LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
            int currentHour = nowTime.getHour();
            Integer usualHour = profile.getUsualTransactionHour();

            boolean isNightWindow = (currentHour >= 1 && currentHour <= 5);
            boolean isDeviatedHour = usualHour != null && Math.abs(currentHour - usualHour) > 8;

            if (isNightWindow || isDeviatedHour) {
                FraudRule rule = ruleMap.get("UNUSUAL_TIME");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("Unusual transaction time");
            }
        }

        // 7. PREVIOUS_FRAUD Rule
        // Only count genuinely BLOCKED transactions (confirmed fraud) — not FAILED
        // (e.g. insufficient balance)
        // or VERIFICATION_REQUIRED (pending OTP), which are legitimate in-progress
        // operations.
        if (ruleMap.containsKey("PREVIOUS_FRAUD")) {
            boolean hasPreviousFraud = (profile.getPreviousFraudCount() > 0) || senderTxHistory.stream()
                    .anyMatch(tx -> tx.getStatus() == TransactionStatus.BLOCKED
                            && tx.getDescription() != null
                            && tx.getDescription().contains("Fraud Analyst")); // Only analyst-confirmed blocks

            if (hasPreviousFraud) {
                FraudRule rule = ruleMap.get("PREVIOUS_FRAUD");
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleCode());
                riskReasons.add("Previous fraud history");
            }
        }

        // Risk Score Capped at 100
        int finalScore = Math.min(totalRiskScore, 100);

        // Determine Risk Level & Decision
        String riskLevel;
        String decision;

        if (finalScore <= 30) {
            riskLevel = "LOW";
            decision = "ALLOW";
        } else if (finalScore <= 70) {
            riskLevel = "MEDIUM";
            decision = "OTP_REQUIRED";
        } else {
            riskLevel = "HIGH";
            decision = "VERIFICATION_REQUIRED";
        }

        logger.info("Evaluated transaction for {}: Score={}, Level={}, Decision={}, Reasons={}",
                sender.getUsername(), finalScore, riskLevel, decision, riskReasons);

        return FraudEvaluationResult.builder()
                .riskScore(finalScore)
                .riskLevel(riskLevel)
                .riskReasons(riskReasons)
                .decision(decision)
                .triggeredRules(triggeredRules)
                .build();
    }

    @Override
    @Transactional
    public void recordFraudAlert(Transaction transaction, FraudEvaluationResult evaluationResult) {
        if ("ALLOW".equals(evaluationResult.getDecision()) && evaluationResult.getRiskScore() <= 30) {
            return;
        }

        String joinedReasons = String.join("; ", evaluationResult.getRiskReasons());

        FraudAlert alert = FraudAlert.builder()
                .transaction(transaction)
                .user(transaction.getSender())
                .riskScore(evaluationResult.getRiskScore())
                .riskLevel(evaluationResult.getRiskLevel())
                .riskReasons(joinedReasons.isEmpty() ? "Risk score threshold reached" : joinedReasons)
                .decision(evaluationResult.getDecision())
                .status("OPEN")
                .build();

        fraudAlertRepository.save(alert);

        // Track fraud flag in profile
        boolean isHighRiskFraud = "VERIFICATION_REQUIRED".equals(evaluationResult.getDecision());
        userRiskProfileService.updateProfileAfterTransaction(
                transaction.getSender(),
                transaction.getAmount(),
                transaction.getDeviceId(),
                transaction.getLocation(),
                isHighRiskFraud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRuleDto> getAllRules() {
        return fraudRuleRepository.findAll().stream()
                .map(this::mapToFraudRuleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FraudRuleDto toggleRule(Long ruleId, boolean enabled) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with id: " + ruleId));
        rule.setEnabled(enabled);
        FraudRule updatedRule = fraudRuleRepository.save(rule);
        return mapToFraudRuleDto(updatedRule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlertDto> getAllAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToFraudAlertDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FraudAlertDto updateAlertStatus(Long alertId, String status) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud alert not found with id: " + alertId));
        alert.setStatus(status);
        FraudAlert saved = fraudAlertRepository.save(alert);
        logger.info("Fraud alert {} status updated to {} by analyst.", alertId, status);
        return mapToFraudAlertDto(saved);
    }

    @Override
    @Transactional
    public FraudRuleDto createRule(FraudRuleDto dto) {
        String name = dto.getRuleName() != null ? dto.getRuleName() : dto.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be empty");
        }

        String code = dto.getRuleCode();
        if (code == null || code.trim().isEmpty()) {
            code = name.toUpperCase().replaceAll("[^A-Z0-9_]", "_").replaceAll("_+", "_");
            if (code.endsWith("_")) {
                code = code.substring(0, code.length() - 1);
            }
            if (code.startsWith("_")) {
                code = code.substring(1);
            }
        }

        if (fraudRuleRepository.existsByRuleCode(code)) {
            throw new IllegalArgumentException("Fraud rule code already exists: " + code);
        }

        Integer points = dto.getRiskPoints() != null ? dto.getRiskPoints() : dto.getPoints();
        if (points == null) {
            points = 0;
        } else if (points < 0 || points > 100) {
            throw new IllegalArgumentException("Risk points must be between 0 and 100");
        }

        FraudRule rule = FraudRule.builder()
                .ruleCode(code)
                .ruleName(name)
                .description(dto.getDescription())
                .riskPoints(points)
                .enabled(dto.isEnabled())
                .build();

        FraudRule saved = fraudRuleRepository.save(rule);
        logger.info("Created new fraud rule: {}", code);
        return mapToFraudRuleDto(saved);
    }

    @Override
    @Transactional
    public FraudRuleDto updateRule(Long id, FraudRuleDto dto) {
        FraudRule rule = fraudRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with id: " + id));

        String name = dto.getRuleName() != null ? dto.getRuleName() : dto.getName();
        if (name != null && !name.trim().isEmpty()) {
            rule.setRuleName(name);
        }

        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription());
        }

        Integer points = dto.getRiskPoints() != null ? dto.getRiskPoints() : dto.getPoints();
        if (points != null) {
            if (points < 0 || points > 100) {
                throw new IllegalArgumentException("Risk points must be between 0 and 100");
            }
            rule.setRiskPoints(points);
        }

        rule.setEnabled(dto.isEnabled());

        FraudRule saved = fraudRuleRepository.save(rule);
        logger.info("Updated fraud rule with id: {}", id);
        return mapToFraudRuleDto(saved);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        FraudRule rule = fraudRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with id: " + id));
        fraudRuleRepository.delete(rule);
        logger.info("Deleted fraud rule with id: {}", id);
    }

    private FraudRuleDto mapToFraudRuleDto(FraudRule rule) {
        return FraudRuleDto.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .riskPoints(rule.getRiskPoints())
                .enabled(rule.isEnabled())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private FraudAlertDto mapToFraudAlertDto(FraudAlert alert) {
        return FraudAlertDto.builder()
                .id(alert.getId())
                .transactionId(alert.getTransaction().getId())
                .referenceCode(alert.getTransaction().getReferenceCode())
                .senderUsername(alert.getUser().getUsername())
                .receiverUsername(alert.getTransaction().getReceiver().getUsername())
                .amount(alert.getTransaction().getAmount())
                .riskScore(alert.getRiskScore())
                .riskLevel(alert.getRiskLevel())
                .riskReasons(alert.getRiskReasons())
                .decision(alert.getDecision())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
