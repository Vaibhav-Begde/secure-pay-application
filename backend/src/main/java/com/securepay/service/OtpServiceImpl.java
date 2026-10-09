package com.securepay.service;

import com.securepay.dto.OtpSendRequest;
import com.securepay.dto.OtpVerifyRequest;
import com.securepay.dto.TransactionDto;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.model.OtpVerification;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.repository.OtpVerificationRepository;
import com.securepay.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final OtpVerificationRepository otpVerificationRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;
    private final EmailService emailService;

    @Autowired
    public OtpServiceImpl(OtpVerificationRepository otpVerificationRepository,
                          TransactionRepository transactionRepository,
                          PasswordEncoder passwordEncoder,
                          @Lazy TransactionService transactionService,
                          EmailService emailService) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionService = transactionService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void generateAndSendOtp(Transaction transaction) {
        String recipientEmail = transaction.getSender().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalStateException("A registered email address is required to send the verification code.");
        }

        // Generate secure 6-digit OTP code (100000 - 999999)
        int rawCodeNumber = 100000 + secureRandom.nextInt(900000);
        String rawOtp = String.valueOf(rawCodeNumber);

        String hashedOtp = passwordEncoder.encode(rawOtp);
        Instant expiresAt = Instant.now().plusSeconds(300); // 5 minutes validity

        // Mark any previous unused OTPs for this transaction as used
        otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc(transaction.getReferenceCode())
                .ifPresent(previousOtp -> {
                    previousOtp.setUsed(true);
                    otpVerificationRepository.save(previousOtp);
                });

        OtpVerification otpVerification = OtpVerification.builder()
                .transaction(transaction)
                .user(transaction.getSender())
                .otpHash(hashedOtp)
                .expiresAt(expiresAt)
                .attempts(0)
                .used(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        emailService.sendOtpEmail(
                recipientEmail,
                transaction.getSender().getUsername(),
                rawOtp,
                transaction.getReferenceCode(),
                transaction.getAmount()
        );
        logger.info("Generated OTP for Transaction {} and dispatched it to the sender's registered email.",
                transaction.getReferenceCode());
    }

    @Override
    @Transactional
    public TransactionDto sendOtpForReferenceCode(String username, OtpSendRequest sendRequest) {
        Transaction transaction = transactionRepository.findByReferenceCode(sendRequest.getReferenceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference code: " + sendRequest.getReferenceCode()));

        if (!transaction.getSender().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to request OTP for this transaction");
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED || transaction.getStatus() == TransactionStatus.FAILED) {
            throw new IllegalArgumentException("Transaction is already " + transaction.getStatus() + " and cannot be verified");
        }

        generateAndSendOtp(transaction);

        return TransactionDto.builder()
                .id(transaction.getId())
                .referenceCode(transaction.getReferenceCode())
                .senderUsername(transaction.getSender().getUsername())
                .receiverUsername(transaction.getReceiver().getUsername())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .riskScore(transaction.getRiskScore())
                .riskLevel(transaction.getRiskLevel())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public TransactionDto verifyOtp(String username, OtpVerifyRequest verifyRequest) {
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc(verifyRequest.getReferenceCode())
                .orElseThrow(() -> new IllegalArgumentException("No active OTP found or OTP has already been used/expired. Please request a new OTP."));

        if (!otpVerification.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to verify this OTP");
        }

        // 1. Check Expiration (5 mins)
        if (Instant.now().isAfter(otpVerification.getExpiresAt())) {
            otpVerification.setUsed(true);
            otpVerificationRepository.save(otpVerification);
            throw new IllegalArgumentException("OTP code has expired (5 minute limit). Please request a new OTP.");
        }

        // 2. Check Maximum Attempts (Max 3)
        if (otpVerification.getAttempts() >= 3) {
            otpVerification.setUsed(true);
            otpVerificationRepository.save(otpVerification);
            throw new IllegalArgumentException("Maximum OTP verification attempts exceeded (3/3). Please request a new OTP.");
        }

        // Increment attempt counter
        int attempts = otpVerification.getAttempts() + 1;
        otpVerification.setAttempts(attempts);

        // Match only the one-time code issued for this transaction.
        boolean isMatch = passwordEncoder.matches(verifyRequest.getOtpCode(), otpVerification.getOtpHash());

        if (!isMatch) {
            otpVerificationRepository.save(otpVerification);
            int remaining = 3 - attempts;
            if (remaining <= 0) {
                otpVerification.setUsed(true);
                otpVerificationRepository.save(otpVerification);
                throw new IllegalArgumentException("Invalid OTP code. Maximum verification attempts reached (3/3). Request a new OTP.");
            } else {
                throw new IllegalArgumentException("Invalid OTP code. Remaining attempts: " + remaining);
            }
        }

        // OTP Verified Successfully -> Prevent Reuse
        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);

        logger.info("Successfully verified OTP for Transaction {}", verifyRequest.getReferenceCode());

        // Complete Adaptive Transaction State Transition
        return transactionService.completeOtpVerification(verifyRequest.getReferenceCode());
    }
}
