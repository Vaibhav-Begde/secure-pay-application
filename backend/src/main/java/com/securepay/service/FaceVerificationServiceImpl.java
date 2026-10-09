package com.securepay.service;

import com.securepay.dto.TransactionDto;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import com.securepay.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaceVerificationServiceImpl implements FaceVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public FaceVerificationServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TransactionDto verifyFace(String username, String referenceCode, String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new IllegalArgumentException("Selfie image is required for face verification");
        }

        Transaction transaction = transactionRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference code: " + referenceCode));

        if (!transaction.getSender().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to perform face verification for this transaction");
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Transaction is already completed");
        }

        // Validate that OTP verification was already completed
        String desc = transaction.getDescription();
        if (desc == null || !desc.contains("Awaiting Face Verification")) {
            throw new IllegalArgumentException("OTP verification must be completed successfully before face verification");
        }

        User sender = transaction.getSender();
        if (sender.getReferenceFace() == null || sender.getReferenceFace().trim().isEmpty()) {
            throw new IllegalStateException("User has not configured a reference face. Please configure a face image first.");
        }

        // Mock verification processing delay & logger (do NOT save the base64 image in DB or raw form)
        logger.info("Simulating face verification for transaction reference: {} (Base64 length: {})", 
                referenceCode, base64Image.length());
        
        try {
            // Processing delay
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Face verification interrupted", e);
        }

        // Mock matching logic
        boolean matches = calculateMockMatch(sender.getReferenceFace(), base64Image);
        if (!matches) {
            // Face Verification Failed -> Transition to BLOCKED
            transaction.setStatus(TransactionStatus.BLOCKED);
            transaction.setDescription("Face Verification Failed - Transaction Blocked");
            transactionRepository.save(transaction);
            logger.warn("Face verification failed for transaction {}. Marked as BLOCKED.", referenceCode);
            throw new SecurityException("Face verification failed. Faces do not match.");
        }

        // Face Verification Approved -> Transition to PENDING
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription("OTP & Face Verified - Pending Fraud Analyst Manual Approval");
        Transaction updated = transactionRepository.save(transaction);

        logger.info("Face verification successfully completed and matched. Transaction {} moved to PENDING status.", referenceCode);

        return TransactionDto.builder()
                .id(updated.getId())
                .referenceCode(updated.getReferenceCode())
                .senderUsername(updated.getSender().getUsername())
                .receiverUsername(updated.getReceiver().getUsername())
                .amount(updated.getAmount())
                .status(updated.getStatus())
                .description(updated.getDescription())
                .deviceId(updated.getDeviceId())
                .ipAddress(updated.getIpAddress())
                .location(updated.getLocation())
                .transactionTime(updated.getTransactionTime())
                .riskScore(updated.getRiskScore())
                .riskLevel(updated.getRiskLevel())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void configureFace(String username, String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new IllegalArgumentException("Face image is required for configuration");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        user.setReferenceFace(base64Image);
        userRepository.save(user);

        logger.info("Face reference configured successfully for user: {}", username);
    }

    private boolean calculateMockMatch(String reference, String current) {
        // Mock liveness and face matching
        // In a real application, this would call an external biometric verification service
        // We'll simulate success if lengths are somewhat similar or both are long strings
        if (reference == null || current == null) return false;
        
        // As a very basic mock, just check if they are both valid data URLs
        if (current.startsWith("data:image/") && current.length() > 100) {
            // 95% chance of success for demo purposes if not strictly identical
            return Math.random() < 0.95 || reference.equals(current);
        }
        return false;
    }
}
