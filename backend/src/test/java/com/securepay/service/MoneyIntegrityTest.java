package com.securepay.service;

import com.securepay.dto.TransactionDto;
import com.securepay.exception.InsufficientBalanceException;
import com.securepay.model.*;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import com.securepay.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Phase 2 & 3 — Money Integrity + Analyst Action Tests
 *
 * Covers:
 *  - Negative/zero/decimal amount validation
 *  - Sufficient / insufficient balance
 *  - Self-transfer rejection
 *  - approveTransaction idempotency (H5 fix)
 *  - blockTransaction guards
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Money Integrity & Analyst Action Tests")
class MoneyIntegrityTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private UserRiskProfileService userRiskProfileService;
    @Mock private OtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User sender;
    private User receiver;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L).username("alice").email("alice@example.com")
                .password("hash").transactionPinHash("encoded-pin")
                .role(Role.CUSTOMER).enabled(true).build();

        receiver = User.builder()
                .id(2L).username("bob").email("bob@example.com")
                .password("hash").role(Role.CUSTOMER).enabled(true).build();

        senderWallet = Wallet.builder()
                .id(10L).user(sender).balance(new BigDecimal("10000.00")).currency("INR").build();

        receiverWallet = Wallet.builder()
                .id(20L).user(receiver).balance(new BigDecimal("500.00")).currency("INR").build();

        lenient().when(passwordEncoder.matches("123456", "encoded-pin")).thenReturn(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2: Amount Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Amount Validation — Phase 2")
    class AmountValidation {

        @Test
        @DisplayName("Negative amount is rejected before any fraud check")
        void negativeAmount_IsRejected() {
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("-100.00"))
                    .transactionPin("123456")
                    .build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.transferMoney("alice", req));

            // No money should move, no fraud check
            verify(fraudDetectionService, never()).evaluateTransaction(any(), any(), any(), any(), any());
            verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
        }

        @Test
        @DisplayName("Zero amount is rejected before any fraud check")
        void zeroAmount_IsRejected() {
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(BigDecimal.ZERO)
                    .transactionPin("123456")
                    .build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.transferMoney("alice", req));
            verify(fraudDetectionService, never()).evaluateTransaction(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Decimal precision: 0.01 amount is accepted and handled with BigDecimal")
        void decimalPrecision_OnePayse_IsHandled() {
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("0.01"))
                    .transactionPin("123456")
                    .build();

            com.securepay.dto.FraudEvaluationResult lowRisk = com.securepay.dto.FraudEvaluationResult.builder()
                    .riskScore(5).riskLevel("LOW").decision("ALLOW").build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
            when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
            when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRisk);
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction tx = i.getArgument(0); tx.setId(1L); return tx;
            });

            TransactionDto result = transactionService.transferMoney("alice", req);

            assertEquals(TransactionStatus.COMPLETED, result.getStatus());
            // BigDecimal subtraction: 10000.00 - 0.01 = 9999.99
            assertEquals(new BigDecimal("9999.99"), senderWallet.getBalance());
            assertEquals(new BigDecimal("500.01"), receiverWallet.getBalance());
        }

        @Test
        @DisplayName("Amount exactly at balance boundary: balance == amount succeeds")
        void exactBalanceAmount_Succeeds() {
            senderWallet.setBalance(new BigDecimal("250.00"));
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("250.00"))
                    .transactionPin("123456")
                    .build();

            com.securepay.dto.FraudEvaluationResult lowRisk = com.securepay.dto.FraudEvaluationResult.builder()
                    .riskScore(5).riskLevel("LOW").decision("ALLOW").build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
            when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
            when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRisk);
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction tx = i.getArgument(0); tx.setId(1L); return tx;
            });

            TransactionDto result = transactionService.transferMoney("alice", req);
            assertEquals(TransactionStatus.COMPLETED, result.getStatus());
            assertEquals(BigDecimal.ZERO.setScale(2), senderWallet.getBalance().setScale(2));
        }

        @Test
        @DisplayName("Amount one paisa over balance is rejected with InsufficientBalanceException")
        void amountOnePayseOverBalance_IsRejected() {
            senderWallet.setBalance(new BigDecimal("250.00"));
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("250.01"))
                    .transactionPin("123456")
                    .build();

            com.securepay.dto.FraudEvaluationResult lowRisk = com.securepay.dto.FraudEvaluationResult.builder()
                    .riskScore(5).riskLevel("LOW").decision("ALLOW").build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
            when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
            when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRisk);
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));

            assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.transferMoney("alice", req));

            // Balance must be unchanged
            assertEquals(new BigDecimal("250.00"), senderWallet.getBalance());
            assertEquals(new BigDecimal("500.00"), receiverWallet.getBalance());
        }

        @Test
        @DisplayName("Client-submitted balance field is ignored — backend reads authoritative balance")
        void clientSubmittedBalance_IsIgnored() {
            // TransferRequest has no balance field — this verifies the server always reads from DB wallet
            // Even if a tampered client submits fake balance, the WalletRepository is the source of truth
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("500.00"))
                    .transactionPin("123456")
                    .build();

            // Sender actual balance in DB: 100 (insufficient)
            senderWallet.setBalance(new BigDecimal("100.00"));

            com.securepay.dto.FraudEvaluationResult lowRisk = com.securepay.dto.FraudEvaluationResult.builder()
                    .riskScore(5).riskLevel("LOW").decision("ALLOW").build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
            when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
            when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRisk);
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));

            // Transfer should fail regardless of what the client claims
            assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.transferMoney("alice", req));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3: Analyst approveTransaction H5 Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Analyst approveTransaction — H5 Double-Approval Guard")
    class ApproveTransactionGuard {

        private Transaction pendingTx;

        @BeforeEach
        void setUpTx() {
            pendingTx = Transaction.builder()
                    .id(99L).referenceCode("TRX-TEST99")
                    .sender(sender).receiver(receiver)
                    .amount(new BigDecimal("5000.00"))
                    .status(TransactionStatus.PENDING)
                    .riskScore(80).riskLevel("HIGH").build();
        }

        @Test
        @DisplayName("Approve PENDING transaction: moves money and sets COMPLETED")
        void approvePending_MovesMoneyAndCompletes() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransactionDto result = transactionService.approveTransaction(99L);

            assertEquals(TransactionStatus.COMPLETED, result.getStatus());
            assertEquals(new BigDecimal("5000.00"), senderWallet.getBalance());   // 10000 - 5000
            assertEquals(new BigDecimal("5500.00"), receiverWallet.getBalance()); // 500 + 5000
            verify(walletRepository).save(senderWallet);
            verify(walletRepository).save(receiverWallet);
        }

        @Test
        @DisplayName("H5: Approve already-COMPLETED transaction is idempotent — no second balance movement")
        void approveAlreadyCompleted_IsIdempotentNoDoubleCredit() {
            pendingTx.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));

            TransactionDto result = transactionService.approveTransaction(99L);

            assertEquals(TransactionStatus.COMPLETED, result.getStatus());
            // No wallet operations should occur
            verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("H5: Approve BLOCKED transaction throws IllegalArgumentException")
        void approveBlocked_ThrowsIllegalArgument() {
            pendingTx.setStatus(TransactionStatus.BLOCKED);
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.approveTransaction(99L));
            verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
        }

        @Test
        @DisplayName("H5: Approve FAILED transaction throws IllegalArgumentException")
        void approveFailed_ThrowsIllegalArgument() {
            pendingTx.setStatus(TransactionStatus.FAILED);
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.approveTransaction(99L));
        }

        @Test
        @DisplayName("H5: Insufficient balance at approval time marks FAILED, throws exception")
        void approveWithInsufficientBalance_MarksFailed() {
            senderWallet.setBalance(new BigDecimal("100.00")); // less than 5000
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.approveTransaction(99L));

            assertEquals(TransactionStatus.FAILED, pendingTx.getStatus());
            // Receiver balance must not increase
            assertEquals(new BigDecimal("500.00"), receiverWallet.getBalance());
        }

        @Test
        @DisplayName("Block COMPLETED transaction throws IllegalArgumentException")
        void blockCompleted_ThrowsIllegalArgument() {
            pendingTx.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.blockTransaction(99L));
        }

        @Test
        @DisplayName("Block already-BLOCKED transaction is idempotent")
        void blockAlreadyBlocked_IsIdempotent() {
            pendingTx.setStatus(TransactionStatus.BLOCKED);
            pendingTx.setDescription("Blocked by Fraud Analyst");
            when(transactionRepository.findById(99L)).thenReturn(Optional.of(pendingTx));

            TransactionDto result = transactionService.blockTransaction(99L);
            assertEquals(TransactionStatus.BLOCKED, result.getStatus());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3: Request Tampering
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Request Tampering — Sender Identity")
    class RequestTampering {

        @Test
        @DisplayName("Sender identity is taken from JWT username, not request body")
        void senderIdentityFromJwt_NotRequestBody() {
            // Even if the request body had a different sender, the service always
            // uses the authenticated username parameter (which comes from the JWT)
            com.securepay.dto.TransferRequest req = com.securepay.dto.TransferRequest.builder()
                    .receiverUsernameOrEmail("bob")
                    .amount(new BigDecimal("100.00"))
                    .transactionPin("123456")
                    .build();

            com.securepay.dto.FraudEvaluationResult lowRisk = com.securepay.dto.FraudEvaluationResult.builder()
                    .riskScore(5).riskLevel("LOW").decision("ALLOW").build();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
            when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
            when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRisk);
            when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
            when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction tx = i.getArgument(0); tx.setId(1L); return tx;
            });

            TransactionDto result = transactionService.transferMoney("alice", req);

            // Verify sender is alice (from JWT param) — not any tampered body value
            assertEquals("alice", result.getSenderUsername());
            // Verify the service looked up alice specifically from DB (not any other user)
            verify(userRepository).findByUsername("alice");
            verify(userRepository, never()).findByUsername("mallory");
        }
    }
}
