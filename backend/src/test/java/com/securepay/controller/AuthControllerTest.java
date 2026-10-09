package com.securepay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.dto.AuthResponse;
import com.securepay.dto.LoginRequest;
import com.securepay.dto.RegisterRequest;
import com.securepay.dto.UserDto;
import com.securepay.model.Role;
import com.securepay.security.CustomUserDetailsService;
import com.securepay.security.JwtAuthenticationEntryPoint;
import com.securepay.security.JwtTokenProvider;
import com.securepay.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void registerUser_ReturnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .build();

        UserDto responseUser = UserDto.builder()
                .id(2L)
                .username("jane_doe")
                .email("jane@example.com")
                .role(Role.CUSTOMER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(responseUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("jane_doe"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    void loginUser_ReturnsOkWithJwtToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane_doe")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.header.payload.signature")
                .tokenType("Bearer")
                .id(2L)
                .username("jane_doe")
                .email("jane@example.com")
                .role(Role.CUSTOMER)
                .message("Login successful")
                .build();

        when(authService.loginUser(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.header.payload.signature"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("jane_doe"));
    }
}
