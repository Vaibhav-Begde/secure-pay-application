package com.securepay.service;

import com.securepay.dto.AuthResponse;
import com.securepay.dto.LoginRequest;
import com.securepay.dto.RegisterRequest;
import com.securepay.dto.SetTransactionPinRequest;
import com.securepay.dto.UserDto;
import com.securepay.exception.UserAlreadyExistsException;
import com.securepay.model.Role;
import com.securepay.model.User;
import com.securepay.repository.UserRepository;
import com.securepay.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private WalletService walletService;

    @Mock
    private UserRiskProfileService userRiskProfileService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("$2a$10$encodedPasswordHashHere")
                .role(Role.CUSTOMER)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        registerRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("secret123")
                .role(Role.CUSTOMER)
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("john_doe")
                .password("secret123")
                .build();
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encodedPasswordHashHere");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDto userDto = authService.registerUser(registerRequest);

        assertNotNull(userDto);
        assertEquals("john_doe", userDto.getUsername());
        assertEquals("john@example.com", userDto.getEmail());
        assertEquals(Role.CUSTOMER, userDto.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameOrEmail("john_doe", "john_doe"))
                .thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateToken(any(), eq(1L), eq("CUSTOMER")))
                .thenReturn("mocked.jwt.token");

        AuthResponse response = authService.loginUser(loginRequest, "127.0.0.1", "Mozilla/5.0 Chrome/1.0 Windows");

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("john_doe", response.getUsername());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertFalse(response.isTransactionPinSet());
    }

    @Test
    void setTransactionPin_ConfirmsPasswordAndStoresHash() {
        SetTransactionPinRequest request = new SetTransactionPinRequest();
        request.setCurrentPassword("secret123");
        request.setTransactionPin("654321");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", sampleUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("hashed-pin");

        authService.setTransactionPin("john_doe", request);

        assertEquals("hashed-pin", sampleUser.getTransactionPinHash());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void setTransactionPin_RejectsIncorrectPassword() {
        SetTransactionPinRequest request = new SetTransactionPinRequest();
        request.setCurrentPassword("incorrect");
        request.setTransactionPin("654321");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("incorrect", sampleUser.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.setTransactionPin("john_doe", request));
        verify(userRepository, never()).save(any(User.class));
    }
}
