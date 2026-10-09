package com.securepay.service;

import com.securepay.dto.FraudEvaluationResult;
import com.securepay.dto.TransactionDto;
import com.securepay.dto.TransferRequest;
import com.securepay.exception.InsufficientBalanceException;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.model.User;
import com.securepay.model.Wallet;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import com.securepay.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;
    private final UserRiskProfileService userRiskProfileService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TransactionServiceImpl(UserRepository userRepository,
                                   WalletRepository walletRepository,
                                   TransactionRepository transactionRepository,
                                   FraudDetectionService fraudDetectionService,
                                   UserRiskProfileService userRiskProfileService,
                                   @Lazy OtpService otpService,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.userRiskProfileService = userRiskProfileService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public FraudEvaluationResult evaluateTransferRisk(String senderUsername, TransferRequest transferRequest) {
        BigDecimal amount = transferRequest.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive and greater than zero");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));

        User receiver = userRepository.findByUsernameOrEmail(
                transferRequest.getReceiverUsernameOrEmail(),
                transferRequest.getReceiverUsernameOrEmail()
        ).orElseThrow(() -> new ResourceNotFoundException("Receiver user not found with username or email: " + transferRequest.getReceiverUsernameOrEmail()));

        String deviceId = transferRequest.getDeviceId() != null ? transferRequest.getDeviceId() : "Web-Browser-Chrome";
        String location = transferRequest.getLocation() != null ? transferRequest.getLocation() : "New York, USA";

        return fraudDetectionService.evaluateTransaction(sender, receiver, amount, deviceId, location);
    }

    @Override
    @Transactional
    public TransactionDto transferMoney(String senderUsername, TransferRequest transferRequest) {
        BigDecimal amount = transferRequest.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive and greater than zero");
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));

        if (sender.getTransactionPinHash() == null) {
            throw new IllegalArgumentException("Set a transaction PIN before making transfers.");
        }
        if (transferRequest.getTransactionPin() == null
                || !passwordEncoder.matches(transferRequest.getTransactionPin(), sender.getTransactionPinHash())) {
            throw new IllegalArgumentException("Transaction PIN is incorrect.");
        }

        User receiver = userRepository.findByUsernameOrEmail(
                transferRequest.getReceiverUsernameOrEmail(),
                transferRequest.getReceiverUsernameOrEmail()
        ).orElseThrow(() -> new ResourceNotFoundException("Receiver user not found with username or email: " + transferRequest.getReceiverUsernameOrEmail()));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot transfer money to your own wallet");
        }

        String deviceId = transferRequest.getDeviceId() != null ? transferRequest.getDeviceId() : "Web-Browser-Chrome";
        String ipAddress = transferRequest.getIpAddress() != null ? transferRequest.getIpAddress() : "127.0.0.1";
        String location = transferRequest.getLocation() != null ? transferRequest.getLocation() : "New York, USA";
        Instant transactionTime = Instant.now();

        // 1. Evaluate Fraud Risk Rules BEFORE modifying balances
        FraudEvaluationResult evaluation = fraudDetectionService.evaluateTransaction(
                sender,
                receiver,
                amount,
                deviceId,
                location
        );

        logger.info("Fraud Evaluation Result for transfer ₹{} from {} to {}: RiskScore={}, RiskLevel={}, Decision={}",
                amount, sender.getUsername(), receiver.getUsername(), evaluation.getRiskScore(), evaluation.getRiskLevel(), evaluation.getDecision());

        // 2. Handle HIGH Risk (Blocked / Analyst Verification Required)
        if ("VERIFICATION_REQUIRED".equals(evaluation.getDecision()) && evaluation.getRiskScore() > 70) {
            Transaction blockedTx = Transaction.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .amount(amount)
                    .status(TransactionStatus.BLOCKED)
                    .description(transferRequest.getDescription() != null ? transferRequest.getDescription() : "High Risk Transfer - Blocked")
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .location(location)
                    .transactionTime(transactionTime)
                    .riskScore(evaluation.getRiskScore())
                    .riskLevel(evaluation.getRiskLevel())
                    .build();

            Transaction savedBlockedTx = transactionRepository.save(blockedTx);
            fraudDetectionService.recordFraudAlert(savedBlockedTx, evaluation);

            // Automatically generate OTP for high risk challenge
            otpService.generateAndSendOtp(savedBlockedTx);

            return mapToTransactionDto(savedBlockedTx);
        }

        // 3. Handle MEDIUM Risk (Step-Up OTP Challenge Required)
        if ("OTP_REQUIRED".equals(evaluation.getDecision())) {
            Transaction otpTx = Transaction.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .amount(amount)
                    .status(TransactionStatus.VERIFICATION_REQUIRED)
                    .description(transferRequest.getDescription() != null ? transferRequest.getDescription() : "Medium Risk Transfer - OTP Required")
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .location(location)
                    .transactionTime(transactionTime)
                    .riskScore(evaluation.getRiskScore())
                    .riskLevel(evaluation.getRiskLevel())
                    .build();

            Transaction savedOtpTx = transactionRepository.save(otpTx);
            fraudDetectionService.recordFraudAlert(savedOtpTx, evaluation);

            // Automatically generate OTP for medium risk challenge
            otpService.generateAndSendOtp(savedOtpTx);

            return mapToTransactionDto(savedOtpTx);
        }

        // 4. Handle LOW Risk (Decision: ALLOW) -> Complete Money Transfer
        Wallet senderWallet = walletRepository.findByUserIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        Wallet receiverWallet = walletRepository.findByUserIdForUpdate(receiver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            Transaction failedTx = Transaction.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .amount(amount)
                    .status(TransactionStatus.FAILED)
                    .description("Failed: Insufficient balance. Available: ₹" + senderWallet.getBalance())
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .location(location)
                    .transactionTime(transactionTime)
                    .riskScore(evaluation.getRiskScore())
                    .riskLevel(evaluation.getRiskLevel())
                    .build();
            transactionRepository.save(failedTx);

            throw new InsufficientBalanceException("Insufficient balance in wallet. Available: ₹" + senderWallet.getBalance() + ", Requested: ₹" + amount);
        }

        // Update balances atomically
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction completedTx = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description(transferRequest.getDescription() != null ? transferRequest.getDescription() : "Money Transfer")
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .location(location)
                .transactionTime(transactionTime)
                .riskScore(evaluation.getRiskScore())
                .riskLevel(evaluation.getRiskLevel())
                .build();

        Transaction savedCompletedTx = transactionRepository.save(completedTx);

        // Update user risk profile after completed transaction
        userRiskProfileService.updateProfileAfterTransaction(sender, amount, deviceId, location, false);

        return mapToTransactionDto(savedCompletedTx);
    }

    @Override
    @Transactional
    public TransactionDto completeOtpVerification(String referenceCode) {
        Transaction transaction = transactionRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference code: " + referenceCode));

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            return mapToTransactionDto(transaction);
        }

        // Adaptive Handling for HIGH Risk vs MEDIUM Risk
        if ("HIGH".equals(transaction.getRiskLevel()) || transaction.getStatus() == TransactionStatus.BLOCKED) {
            // For HIGH Risk: OTP verification requires an additional Face Verification step
            transaction.setDescription("OTP Verified - Awaiting Face Verification");
            Transaction updatedTx = transactionRepository.save(transaction);

            logger.info("High-risk Transaction {} OTP verified by user. Awaiting face verification step.", referenceCode);
            return mapToTransactionDto(updatedTx);
        }

        // For MEDIUM Risk: OTP verification completes the money transfer
        User sender = transaction.getSender();
        User receiver = transaction.getReceiver();
        BigDecimal amount = transaction.getAmount();

        Wallet senderWallet = walletRepository.findByUserIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        Wallet receiverWallet = walletRepository.findByUserIdForUpdate(receiver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("Failed post-OTP: Insufficient balance. Available: ₹" + senderWallet.getBalance());
            Transaction failedTx = transactionRepository.save(transaction);
            throw new InsufficientBalanceException("Insufficient balance in wallet. Available: ₹" + senderWallet.getBalance() + ", Requested: ₹" + amount);
        }

        // Deduct sender balance and add to receiver balance
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription("Money Transfer (OTP Verified)");
        Transaction completedTx = transactionRepository.save(transaction);

        // Update user risk profile
        userRiskProfileService.updateProfileAfterTransaction(
                sender,
                amount,
                transaction.getDeviceId(),
                transaction.getLocation(),
                false
        );

        logger.info("Medium-risk Transaction {} OTP verified and COMPLETED successfully. Transferred ₹{} from {} to {}",
                referenceCode, amount, sender.getUsername(), receiver.getUsername());

        return mapToTransactionDto(completedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Transaction> transactions = transactionRepository.findUserTransactionHistory(user.getId());

        return transactions.stream()
                .filter(tx -> tx.getSender().getId().equals(user.getId())
                        || tx.getStatus() == TransactionStatus.COMPLETED)
                .map(this::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionDto approveTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // H5 fix: Idempotency guard — prevent double-approval and duplicate money movement
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            logger.warn("Transaction {} is already COMPLETED. Skipping re-approval.", id);
            return mapToTransactionDto(transaction);
        }
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException(
                "Transaction " + id + " cannot be approved from status: " + transaction.getStatus()
                    + ". Only PENDING transactions can be approved."
            );
        }

        User sender = transaction.getSender();
        User receiver = transaction.getReceiver();
        BigDecimal amount = transaction.getAmount();

        Wallet senderWallet = walletRepository.findByUserIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findByUserIdForUpdate(receiver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("Analyst approval failed: Insufficient balance. Available: \u20b9" + senderWallet.getBalance());
            transactionRepository.save(transaction);
            throw new InsufficientBalanceException("Insufficient balance during analyst approval. Available: \u20b9" + senderWallet.getBalance() + ", Required: \u20b9" + amount);
        }

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription("Approved by Fraud Analyst");
        Transaction saved = transactionRepository.save(transaction);
        logger.info("Transaction {} approved by fraud analyst. Amount \u20b9{} transferred.", id, amount);
        return mapToTransactionDto(saved);
    }

    @Override
    @Transactional
    public TransactionDto blockTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Guard: do not re-block or un-complete a transaction
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException(
                "Transaction " + id + " is already COMPLETED and cannot be blocked."
            );
        }
        if (transaction.getStatus() == TransactionStatus.BLOCKED) {
            logger.warn("Transaction {} is already BLOCKED. Skipping.", id);
            return mapToTransactionDto(transaction);
        }

        transaction.setStatus(TransactionStatus.BLOCKED);
        transaction.setDescription("Blocked by Fraud Analyst");
        Transaction saved = transactionRepository.save(transaction);
        logger.info("Transaction {} blocked by fraud analyst.", id);
        return mapToTransactionDto(saved);
    }

    private TransactionDto mapToTransactionDto(Transaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .referenceCode(tx.getReferenceCode())
                .senderUsername(tx.getSender().getUsername())
                .receiverUsername(tx.getReceiver().getUsername())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .deviceId(tx.getDeviceId())
                .ipAddress(tx.getIpAddress())
                .location(tx.getLocation())
                .transactionTime(tx.getTransactionTime())
                .riskScore(tx.getRiskScore())
                .riskLevel(tx.getRiskLevel())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
