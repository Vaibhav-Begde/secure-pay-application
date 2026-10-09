package com.securepay.service;

import com.securepay.dto.AuthResponse;
import com.securepay.dto.LoginRequest;
import com.securepay.dto.RegisterRequest;
import com.securepay.dto.SetTransactionPinRequest;
import com.securepay.dto.UserDto;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.exception.UserAlreadyExistsException;
import com.securepay.model.Role;
import com.securepay.model.User;
import com.securepay.repository.UserRepository;
import com.securepay.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final WalletService walletService;
    private final UserRiskProfileService userRiskProfileService;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           WalletService walletService,
                           UserRiskProfileService userRiskProfileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.walletService = walletService;
        this.userRiskProfileService = userRiskProfileService;
    }

    @Override
    @Transactional
    public UserDto registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + registerRequest.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + registerRequest.getEmail() + "' is already registered");
        }

        // Public registrations are strictly provisioned as CUSTOMER accounts
        Role role = Role.CUSTOMER;

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Automatically initialize Virtual Wallet and UserRiskProfile for new user
        walletService.createInitialWalletForUser(savedUser, new java.math.BigDecimal("1000.00"));
        userRiskProfileService.getOrCreateProfile(savedUser);

        return mapToUserDto(savedUser);
    }

    @Override
    public AuthResponse loginUser(LoginRequest loginRequest, String clientIp, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastLoginAt(java.time.Instant.now());
        user.setLastLoginDevice(describeDevice(userAgent));
        user.setLastLoginIp(clientIp);
        user.setLastLoginLocation(describeLocation(clientIp));
        userRepository.save(user);

        String token = tokenProvider.generateToken(authentication, user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .transactionPinSet(user.getTransactionPinHash() != null)
                .message("Login successful")
                .build();
    }

    @Override
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToUserDto(user);
    }

    @Override
    @Transactional
    public void setTransactionPin(String username, SetTransactionPinRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current account password is incorrect.");
        }

        user.setTransactionPinHash(passwordEncoder.encode(request.getTransactionPin()));
        userRepository.save(user);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .transactionPinSet(user.getTransactionPinHash() != null)
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginDevice(user.getLastLoginDevice())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginLocation(user.getLastLoginLocation())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String describeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown device";
        String browser = userAgent.contains("Edg/") ? "Microsoft Edge"
                : userAgent.contains("Chrome/") ? "Google Chrome"
                : userAgent.contains("Firefox/") ? "Mozilla Firefox"
                : userAgent.contains("Safari/") ? "Safari" : "Web browser";
        String os = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS") ? "macOS"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS"
                : userAgent.contains("Linux") ? "Linux" : "Unknown OS";
        return browser + " on " + os;
    }

    private String describeLocation(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return "Unavailable";
        if (clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.equals("::1")) {
            return "Local development device";
        }
        return "Location unavailable";
    }
}
