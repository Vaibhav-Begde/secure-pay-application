package com.securepay.service;

import com.securepay.dto.OtpVerifyRequest;
import com.securepay.dto.TransactionDto;
import com.securepay.model.*;
import com.securepay.repository.OtpVerificationRepository;
import com.securepay.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpServiceImpl otpService;

    private User sender;
    private Transaction transaction;
    private OtpVerification validOtp;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(1L).username("alice").email("alice@example.com").role(Role.CUSTOMER).build();
        User receiver = User.builder().id(2L).username("bob").email("bob@example.com").role(Role.CUSTOMER).build();

        transaction = Transaction.builder()
                .id(10L)
                .referenceCode("TRX-TEST123")
                .sender(sender)
                .receiver(receiver)
                .amount(new BigDecimal("300.00"))
                .status(TransactionStatus.VERIFICATION_REQUIRED)
                .riskScore(50)
                .riskLevel("MEDIUM")
                .build();

        validOtp = OtpVerification.builder()
                .id(100L)
                .transaction(transaction)
                .user(sender)
                .otpHash("$2a$10$hashedOtpCodeHere")
                .expiresAt(Instant.now().plusSeconds(300))
                .attempts(0)
                .used(false)
                .build();
    }

    @Test
    void generateAndSendOtp_Creates6DigitOtp() {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedOtpCodeHere");
        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.empty());

        otpService.generateAndSendOtp(transaction);

        verify(emailService).sendOtpEmail(
                eq(sender.getEmail()),
                eq(sender.getUsername()),
                argThat(code -> code.matches("\\d{6}")),
                eq(transaction.getReferenceCode()),
                eq(transaction.getAmount())
        );
        verify(otpVerificationRepository, times(1)).save(any(OtpVerification.class));
    }

    @Test
    void generateAndSendOtp_WithoutRegisteredEmail_FailsWithoutCreatingOtp() {
        sender.setEmail(null);

        assertThrows(IllegalStateException.class, () -> otpService.generateAndSendOtp(transaction));

        verify(otpVerificationRepository, never()).save(any(OtpVerification.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void verifyOtp_Success() {
        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .referenceCode("TRX-TEST123")
                .otpCode("654321")
                .build();

        TransactionDto completedDto = TransactionDto.builder()
                .id(10L)
                .referenceCode("TRX-TEST123")
                .senderUsername("alice")
                .receiverUsername("bob")
                .amount(new BigDecimal("300.00"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.of(validOtp));
        when(passwordEncoder.matches("654321", "$2a$10$hashedOtpCodeHere")).thenReturn(true);
        when(transactionService.completeOtpVerification("TRX-TEST123")).thenReturn(completedDto);

        TransactionDto result = otpService.verifyOtp("alice", request);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertTrue(validOtp.isUsed());
        verify(otpVerificationRepository, times(1)).save(validOtp);
    }

    @Test
    void verifyOtp_DefaultTestCode_IsRejected() {
        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .referenceCode("TRX-TEST123")
                .otpCode("123456")
                .build();

        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.of(validOtp));
        when(passwordEncoder.matches("123456", "$2a$10$hashedOtpCodeHere")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> otpService.verifyOtp("alice", request));
        verify(transactionService, never()).completeOtpVerification(anyString());
    }

    @Test
    void verifyOtp_IncorrectCode_ThrowsExceptionAndIncrementsAttempts() {
        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .referenceCode("TRX-TEST123")
                .otpCode("000000")
                .build();

        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.of(validOtp));
        when(passwordEncoder.matches("000000", "$2a$10$hashedOtpCodeHere")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> otpService.verifyOtp("alice", request));

        assertEquals(1, validOtp.getAttempts());
        assertFalse(validOtp.isUsed());
        verify(otpVerificationRepository, times(1)).save(validOtp);
    }

    @Test
    void verifyOtp_MaxAttemptsExceeded_ThrowsException() {
        validOtp.setAttempts(3);

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .referenceCode("TRX-TEST123")
                .otpCode("123456")
                .build();

        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.of(validOtp));

        assertThrows(IllegalArgumentException.class, () -> otpService.verifyOtp("alice", request));
        assertTrue(validOtp.isUsed());
    }

    @Test
    void verifyOtp_ExpiredOtp_ThrowsException() {
        validOtp.setExpiresAt(Instant.now().minusSeconds(10)); // Expired 10 seconds ago

        OtpVerifyRequest request = OtpVerifyRequest.builder()
                .referenceCode("TRX-TEST123")
                .otpCode("123456")
                .build();

        when(otpVerificationRepository.findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc("TRX-TEST123"))
                .thenReturn(Optional.of(validOtp));

        assertThrows(IllegalArgumentException.class, () -> otpService.verifyOtp("alice", request));
        assertTrue(validOtp.isUsed());
    }
}
