package com.securepay.controller;

import com.securepay.security.CustomUserDetailsService;
import com.securepay.security.JwtAccessDeniedHandler;
import com.securepay.security.JwtAuthenticationEntryPoint;
import com.securepay.security.JwtAuthenticationFilter;
import com.securepay.security.JwtTokenProvider;
import com.securepay.service.FraudDetectionService;
import com.securepay.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security access control tests.
 *
 * Scenarios:
 *  11. CUSTOMER accessing /api/admin/** → 403 Forbidden
 *  12. FRAUD_ANALYST accessing /api/admin/** → 403 Forbidden
 *  13. ADMIN accessing /api/analyst/** (fraud dashboard) → 200 OK
 */
@WebMvcTest(controllers = {AdminFraudRuleController.class, FraudAnalystController.class})
@Import({com.securepay.config.SecurityConfig.class, JwtAccessDeniedHandler.class, JwtAuthenticationFilter.class})
@DisplayName("Role-Based Access Control Tests")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 11: CUSTOMER accessing /api/admin/** → 403
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    @DisplayName("Scenario 11: CUSTOMER accessing admin fraud-rules API → 403 Forbidden")
    void scenario11_customerAccessingAdminApi_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/fraud-rules"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 12: FRAUD_ANALYST accessing /api/admin/** → 403
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "analyst_user", roles = "FRAUD_ANALYST")
    @DisplayName("Scenario 12: FRAUD_ANALYST accessing admin fraud-rules API → 403 Forbidden")
    void scenario12_fraudAnalystAccessingAdminApi_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/fraud-rules"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 13: ADMIN accessing /api/analyst/** → 200 OK
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    @DisplayName("Scenario 13: ADMIN accessing fraud analyst dashboard → 200 OK")
    void scenario13_adminAccessingFraudDashboard_returns200() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/analyst/dashboard"))
                .andExpect(status().isOk());
    }
}
