package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.DashboardStatsDto;
import com.securepay.dto.FraudAlertDto;
import com.securepay.dto.TransactionDto;
import com.securepay.model.TransactionStatus;
import com.securepay.service.FraudDetectionService;
import com.securepay.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analyst")
@PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
public class FraudAnalystController {

    private final TransactionService transactionService;
    private final FraudDetectionService fraudDetectionService;

    @Autowired
    public FraudAnalystController(TransactionService transactionService,
                                   FraudDetectionService fraudDetectionService) {
        this.transactionService = transactionService;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * GET /api/analyst/dashboard
     * Returns aggregate KPI counts for the fraud analyst dashboard.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        List<TransactionDto> all = transactionService.getAllTransactions();

        long total    = all.size();
        long lowRisk  = all.stream().filter(t -> "LOW".equalsIgnoreCase(t.getRiskLevel())).count();
        long medRisk  = all.stream().filter(t -> "MEDIUM".equalsIgnoreCase(t.getRiskLevel())).count();
        long highRisk = all.stream().filter(t -> "HIGH".equalsIgnoreCase(t.getRiskLevel())).count();
        long blocked  = all.stream().filter(t -> t.getStatus() == TransactionStatus.BLOCKED).count();
        long pending  = all.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();

        DashboardStatsDto stats = new DashboardStatsDto(total, lowRisk, medRisk, highRisk, blocked, pending);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
    }

    /**
     * GET /api/analyst/transactions
     * Returns ALL transactions in the system (analyst-only view).
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getAllTransactions() {
        List<TransactionDto> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success("All transactions retrieved successfully", transactions));
    }

    /**
     * GET /api/analyst/alerts
     * Returns ALL fraud alerts with riskReasons.
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<FraudAlertDto>>> getAllAlerts() {
        List<FraudAlertDto> alerts = fraudDetectionService.getAllAlerts();
        return ResponseEntity.ok(ApiResponse.success("All fraud alerts retrieved successfully", alerts));
    }

    /**
     * PATCH /api/analyst/transactions/{id}/approve
     * Approves a pending/blocked transaction (transfers funds if applicable).
     */
    @PatchMapping("/transactions/{id}/approve")
    public ResponseEntity<ApiResponse<TransactionDto>> approveTransaction(@PathVariable Long id) {
        TransactionDto dto = transactionService.approveTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction approved successfully", dto));
    }

    /**
     * PATCH /api/analyst/transactions/{id}/block
     * Blocks a transaction.
     */
    @PatchMapping("/transactions/{id}/block")
    public ResponseEntity<ApiResponse<TransactionDto>> blockTransaction(@PathVariable Long id) {
        TransactionDto dto = transactionService.blockTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction blocked successfully", dto));
    }

    /**
     * PATCH /api/analyst/alerts/{id}/review
     * Marks a fraud alert as REVIEWED.
     */
    @PatchMapping("/alerts/{id}/review")
    public ResponseEntity<ApiResponse<FraudAlertDto>> reviewAlert(@PathVariable Long id) {
        FraudAlertDto dto = fraudDetectionService.updateAlertStatus(id, "REVIEWED");
        return ResponseEntity.ok(ApiResponse.success("Alert marked as reviewed", dto));
    }
}
