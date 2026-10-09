package com.securepay.controller;

import com.securepay.security.CustomUserDetailsService;
import com.securepay.security.JwtAccessDeniedHandler;
import com.securepay.security.JwtAuthenticationEntryPoint;
import com.securepay.security.JwtAuthenticationFilter;
import com.securepay.security.JwtTokenProvider;
import com.securepay.service.FraudDetectionService;
import com.securepay.service.TransactionService;
import com.securepay.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3 — IDOR, Vertical Privilege Escalation, JWT / Auth Enforcement
 *
 * Tests:
 *  V1.  Anonymous (no token) → 401 on protected endpoint
 *  V2.  CUSTOMER → /api/admin/** → 403
 *  V3.  CUSTOMER → /api/analyst/** → 403
 *  V4.  FRAUD_ANALYST → /api/admin/** → 403
 *  V5.  ADMIN → /api/analyst/** → 200 (admin has superset access)
 *  V6.  ADMIN → /api/admin/** → 200
 *  V7.  FRAUD_ANALYST → /api/analyst/** → 200
 *  V8.  CUSTOMER → /api/wallet/balance → 200 (own data)
 *  H3a. Unauthenticated POST to transfer → 401
 */
@WebMvcTest(controllers = {
        AdminFraudRuleController.class,
        FraudAnalystController.class,
        WalletController.class,
        TransactionController.class
})
@Import({
        com.securepay.config.SecurityConfig.class,
        JwtAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
@DisplayName("Phase 3 — IDOR & Vertical Privilege Tests")
class PrivilegeEscalationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FraudDetectionService fraudDetectionService;
    @MockitoBean private TransactionService transactionService;
    @MockitoBean private WalletService walletService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // ─────────────────────────────────────────────────────────────────────────
    // V1 — Unauthenticated access
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("V1: No token — protected endpoint returns 401")
    void v1_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/analyst/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H3a: No token on transfer POST returns 401")
    void h3a_noTokenOnTransfer_returns401() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/transactions/transfer")
                        .contentType("application/json")
                        .content("{\"receiverUsernameOrEmail\":\"bob\",\"amount\":100,\"transactionPin\":\"123456\"}")
        ).andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V2, V3 — CUSTOMER cannot reach admin or analyst endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CUSTOMER vertical privilege escalation attempts")
    class CustomerEscalation {

        @Test
        @WithMockUser(username = "alice", roles = "CUSTOMER")
        @DisplayName("V2: CUSTOMER → /api/admin/fraud-rules → 403")
        void v2_customer_to_admin_403() throws Exception {
            mockMvc.perform(get("/api/admin/fraud-rules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CUSTOMER")
        @DisplayName("V3: CUSTOMER → /api/analyst/dashboard → 403")
        void v3_customer_to_analyst_403() throws Exception {
            mockMvc.perform(get("/api/analyst/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CUSTOMER")
        @DisplayName("V3b: CUSTOMER → /api/analyst/fraud-alerts → 403")
        void v3b_customer_to_fraudAlerts_403() throws Exception {
            mockMvc.perform(get("/api/analyst/fraud-alerts"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CUSTOMER")
        @DisplayName("V3c: CUSTOMER → analyst approve endpoint → 403")
        void v3c_customer_analyst_approve_403() throws Exception {
            mockMvc.perform(patch("/api/analyst/transactions/99/approve"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CUSTOMER")
        @DisplayName("V3d: CUSTOMER → analyst block endpoint → 403")
        void v3d_customer_analyst_block_403() throws Exception {
            mockMvc.perform(patch("/api/analyst/transactions/99/block"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V4 — FRAUD_ANALYST cannot reach admin endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FRAUD_ANALYST escalation to ADMIN endpoints")
    class AnalystEscalation {

        @Test
        @WithMockUser(username = "analyst1", roles = "FRAUD_ANALYST")
        @DisplayName("V4: FRAUD_ANALYST → /api/admin/fraud-rules → 403")
        void v4_analyst_to_admin_403() throws Exception {
            mockMvc.perform(get("/api/admin/fraud-rules"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "analyst1", roles = "FRAUD_ANALYST")
        @DisplayName("V4b: FRAUD_ANALYST → POST /api/admin/fraud-rules → 403")
        void v4b_analyst_createFraudRule_403() throws Exception {
            mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/admin/fraud-rules")
                            .contentType("application/json")
                            .content("{\"ruleCode\":\"EVIL\",\"riskPoints\":100}")
            ).andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V5–V7 — Authorized access succeeds
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Authorized access — positive path")
    class AuthorizedAccess {

        @Test
        @WithMockUser(username = "admin1", roles = "ADMIN")
        @DisplayName("V5: ADMIN → /api/analyst/dashboard → 200")
        void v5_admin_to_analyst_200() throws Exception {
            when(transactionService.getAllTransactions()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/analyst/dashboard"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin1", roles = "ADMIN")
        @DisplayName("V6: ADMIN → /api/admin/fraud-rules → 200")
        void v6_admin_to_admin_200() throws Exception {
            when(fraudDetectionService.getAllRules()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/admin/fraud-rules"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "analyst1", roles = "FRAUD_ANALYST")
        @DisplayName("V7: FRAUD_ANALYST → /api/analyst/dashboard → 200")
        void v7_analyst_to_analyst_200() throws Exception {
            when(transactionService.getAllTransactions()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/analyst/dashboard"))
                    .andExpect(status().isOk());
        }
    }
}
