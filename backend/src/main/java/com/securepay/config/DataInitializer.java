package com.securepay.config;

import com.securepay.model.FraudRule;
import com.securepay.model.Role;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.model.User;
import com.securepay.model.Wallet;
import com.securepay.repository.FraudRuleRepository;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import com.securepay.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           FraudRuleRepository fraudRuleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedFraudRules();

        if (userRepository.count() > 0) {
            logger.info("Database users already seeded. Skipping user seed.");
            return;
        }

        logger.info("Seeding initial development users, wallets, and transactions...");

        // 1. Create Users
        User customer1 = userRepository.save(User.builder()
                .username("customer1")
                .email("customer1@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        User customer2 = userRepository.save(User.builder()
                .username("customer2")
                .email("customer2@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        User analyst1 = userRepository.save(User.builder()
                .username("analyst1")
                .email("analyst1@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.FRAUD_ANALYST)
                .enabled(true)
                .build());

        User admin1 = userRepository.save(User.builder()
                .username("admin1")
                .email("admin1@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        // 2. Create Wallets
        walletRepository.save(Wallet.builder()
                .user(customer1)
                .balance(new BigDecimal("25000.00"))
                .currency("INR")
                .build());

        walletRepository.save(Wallet.builder()
                .user(customer2)
                .balance(new BigDecimal("15000.00"))
                .currency("INR")
                .build());

        walletRepository.save(Wallet.builder()
                .user(analyst1)
                .balance(new BigDecimal("5000.00"))
                .currency("INR")
                .build());

        walletRepository.save(Wallet.builder()
                .user(admin1)
                .balance(new BigDecimal("100000.00"))
                .currency("INR")
                .build());

        // 3. Create Initial Seed Transaction
        transactionRepository.save(Transaction.builder()
                .sender(customer1)
                .receiver(customer2)
                .amount(new BigDecimal("2500.00"))
                .status(TransactionStatus.COMPLETED)
                .description("Initial Seed Payment")
                .riskScore(15)
                .riskLevel("LOW")
                .build());

        logger.info("Data seeding completed successfully! Seeded users: customer1, customer2, analyst1, admin1 (Default Password: password123)");
    }

    private void seedFraudRules() {
        if (fraudRuleRepository.count() > 0) {
            // Clean up any legacy rule descriptions that still contain "$"
            java.util.List<FraudRule> existingRules = fraudRuleRepository.findAll();
            for (FraudRule rule : existingRules) {
                boolean changed = false;
                if (rule.getDescription() != null && rule.getDescription().contains("$")) {
                    rule.setDescription(rule.getDescription()
                            .replace("$1,000.00", "₹10,000.00")
                            .replace("$1000", "₹10,000")
                            .replace("$5000", "₹50,000")
                            .replace("$50,000", "₹50,000")
                            .replace("$", "₹"));
                    changed = true;
                }
                if (rule.getRuleName() != null && rule.getRuleName().contains("$")) {
                    rule.setRuleName(rule.getRuleName().replace("$", "₹"));
                    changed = true;
                }
                if (changed) {
                    fraudRuleRepository.save(rule);
                    logger.info("Sanitized legacy fraud rule currency to INR for rule: {}", rule.getRuleCode());
                }
            }
            return;
        }

        logger.info("Seeding initial fraud detection rules into database...");

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("HIGH_AMOUNT")
                .ruleName("High Transaction Amount")
                .description("Transaction amount meets or exceeds ₹10,000.00 threshold")
                .riskPoints(20)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("NEW_RECEIVER")
                .ruleName("New / First-Time Receiver")
                .description("Sender has no prior completed transaction history with receiver")
                .riskPoints(15)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("NEW_DEVICE")
                .ruleName("Unrecognized Device Fingerprint")
                .description("Transaction originating from an unrecognized hardware device")
                .riskPoints(20)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("NEW_LOCATION")
                .ruleName("Unusual Geographical Location")
                .description("Transaction originating from a new IP/Geographical region")
                .riskPoints(15)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("RAPID_TRANSACTIONS")
                .ruleName("Rapid Velocity Transfers")
                .description("Multiple transactions initiated within a 5-minute window")
                .riskPoints(20)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("UNUSUAL_TIME")
                .ruleName("Unusual Time Window")
                .description("Transaction initiated during high-risk hours (1:00 AM - 5:00 AM UTC)")
                .riskPoints(10)
                .enabled(true)
                .build());

        fraudRuleRepository.save(FraudRule.builder()
                .ruleCode("PREVIOUS_FRAUD")
                .ruleName("Previous Fraud History")
                .description("Account associated with prior blocked or failed transaction attempts")
                .riskPoints(30)
                .enabled(true)
                .build());

        logger.info("Seeded 7 default configurable fraud rules into database.");
    }
}
