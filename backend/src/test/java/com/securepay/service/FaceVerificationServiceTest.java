package com.securepay.service;

import com.securepay.dto.TransactionDto;
import com.securepay.model.Role;
import com.securepay.model.Transaction;
import com.securepay.model.TransactionStatus;
import com.securepay.model.User;
import com.securepay.repository.TransactionRepository;
import com.securepay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Face Verification Service Tests")
class FaceVerificationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FaceVerificationServiceImpl faceVerificationService;

    private User user;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.CUSTOMER)
                .referenceFace("data:image/jpeg;base64,sample-reference-face-data-string-longer-than-100-characters-for-mock-verification")
                .build();

        transaction = Transaction.builder()
                .id(101L)
                .referenceCode("TRX-TEST-HIGH-01")
                .sender(user)
                .receiver(User.builder().id(2L).username("receiver").build())
                .amount(new BigDecimal("55000.00"))
                .status(TransactionStatus.BLOCKED)
                .description("OTP Verified - Awaiting Face Verification")
                .riskScore(85)
                .riskLevel("HIGH")
                .build();
    }

    @Test
    @DisplayName("Should successfully configure reference face for user")
    void testConfigureFace_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        faceVerificationService.configureFace("testuser", "data:image/jpeg;base64,new-face-data");

        verify(userRepository, times(1)).save(user);
        assertEquals("data:image/jpeg;base64,new-face-data", user.getReferenceFace());
    }

    @Test
    @DisplayName("Should reject face verification when image is empty")
    void testVerifyFace_EmptyImage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                faceVerificationService.verifyFace("testuser", "TRX-TEST-HIGH-01", "")
        );
    }

    @Test
    @DisplayName("Should reject face verification if user is unauthorized for transaction")
    void testVerifyFace_UnauthorizedUser_ThrowsException() {
        when(transactionRepository.findByReferenceCode("TRX-TEST-HIGH-01")).thenReturn(Optional.of(transaction));

        assertThrows(IllegalArgumentException.class, () ->
                faceVerificationService.verifyFace("wronguser", "TRX-TEST-HIGH-01", "data:image/jpeg;base64,valid-sample-image-data-string-with-sufficient-length")
        );
    }

    @Test
    @DisplayName("Should reject face verification if user has not configured reference face")
    void testVerifyFace_NoReferenceFace_ThrowsException() {
        user.setReferenceFace(null);
        when(transactionRepository.findByReferenceCode("TRX-TEST-HIGH-01")).thenReturn(Optional.of(transaction));

        assertThrows(IllegalStateException.class, () ->
                faceVerificationService.verifyFace("testuser", "TRX-TEST-HIGH-01", "data:image/jpeg;base64,valid-sample-image-data-string-with-sufficient-length")
        );
    }

    @Test
    @DisplayName("Should successfully verify face and transition transaction to PENDING status")
    void testVerifyFace_Success() {
        when(transactionRepository.findByReferenceCode("TRX-TEST-HIGH-01")).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String testCapture = "data:image/jpeg;base64,sample-reference-face-data-string-longer-than-100-characters-for-mock-verification";
        TransactionDto result = faceVerificationService.verifyFace("testuser", "TRX-TEST-HIGH-01", testCapture);

        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertTrue(result.getDescription().contains("OTP & Face Verified"));
        verify(transactionRepository, times(1)).save(transaction);
    }
}
