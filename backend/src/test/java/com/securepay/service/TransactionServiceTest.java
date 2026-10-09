package com.securepay.service;

import com.securepay.dto.TransactionDto;
import com.securepay.dto.TransferRequest;
import com.securepay.exception.InsufficientBalanceException;
import com.securepay.model.Role;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.model.User;
import com.securepay.model.Wallet;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import com.securepay.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private UserRiskProfileService userRiskProfileService;

    @Mock
    private OtpService otpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User sender;
    private User receiver;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("hash")
                .transactionPinHash("encoded-pin")
                .role(Role.CUSTOMER)
                .build();

        receiver = User.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .password("hash")
                .role(Role.CUSTOMER)
                .build();

        senderWallet = Wallet.builder()
                .id(10L)
                .user(sender)
                .balance(new BigDecimal("500.00"))
                .currency("INR")
                .build();

        receiverWallet = Wallet.builder()
                .id(20L)
                .user(receiver)
                .balance(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        lenient().when(passwordEncoder.matches("123456", "encoded-pin")).thenReturn(true);
    }

    @Test
    void transferMoney_Success() {
        TransferRequest request = TransferRequest.builder()
                .receiverUsernameOrEmail("bob")
                .amount(new BigDecimal("150.00"))
                .transactionPin("123456")
                .description("Rent payment")
                .build();

        com.securepay.dto.FraudEvaluationResult lowRiskResult = com.securepay.dto.FraudEvaluationResult.builder()
                .riskScore(15)
                .riskLevel("LOW")
                .decision("ALLOW")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
        when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRiskResult);
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        TransactionDto result = transactionService.transferMoney("alice", request);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals("alice", result.getSenderUsername());
        assertEquals("bob", result.getReceiverUsername());
        assertEquals(new BigDecimal("150.00"), result.getAmount());

        // Verify balance updates
        assertEquals(new BigDecimal("350.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("250.00"), receiverWallet.getBalance());

        verify(walletRepository, times(1)).save(senderWallet);
        verify(walletRepository, times(1)).save(receiverWallet);
    }

    @Test
    void transferMoney_InsufficientBalance_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .receiverUsernameOrEmail("bob")
                .amount(new BigDecimal("600.00"))
                .transactionPin("123456")
                .description("Overdraft attempt")
                .build();

        com.securepay.dto.FraudEvaluationResult lowRiskResult = com.securepay.dto.FraudEvaluationResult.builder()
                .riskScore(15)
                .riskLevel("LOW")
                .decision("ALLOW")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(receiver));
        when(fraudDetectionService.evaluateTransaction(any(), any(), any(), any(), any())).thenReturn(lowRiskResult);
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(receiverWallet));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transferMoney("alice", request));

        // Balance should remain unchanged
        assertEquals(new BigDecimal("500.00"), senderWallet.getBalance());

        // Failed transaction should be recorded
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transferMoney_SelfTransfer_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .receiverUsernameOrEmail("alice")
                .amount(new BigDecimal("50.00"))
                .transactionPin("123456")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsernameOrEmail("alice", "alice")).thenReturn(Optional.of(sender));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferMoney("alice", request));
    }

    @Test
    void transferMoney_WithoutConfiguredPin_IsRejected() {
        sender.setTransactionPinHash(null);
        TransferRequest request = TransferRequest.builder()
                .receiverUsernameOrEmail("bob")
                .amount(new BigDecimal("50.00"))
                .transactionPin("123456")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferMoney("alice", request));
        verify(fraudDetectionService, never()).evaluateTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void transferMoney_WrongPin_IsRejectedBeforeTransfer() {
        TransferRequest request = TransferRequest.builder()
                .receiverUsernameOrEmail("bob")
                .amount(new BigDecimal("50.00"))
                .transactionPin("000000")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("000000", "encoded-pin")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferMoney("alice", request));
        verify(fraudDetectionService, never()).evaluateTransaction(any(), any(), any(), any(), any());
    }
}
