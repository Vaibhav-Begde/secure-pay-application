package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.TransactionDto;
import com.securepay.dto.TransferRequest;
import com.securepay.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<com.securepay.dto.FraudEvaluationResult>> evaluateRisk(@Valid @RequestBody TransferRequest transferRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderUsername = authentication.getName();

        com.securepay.dto.FraudEvaluationResult evaluation = transactionService.evaluateTransferRisk(senderUsername, transferRequest);

        return ResponseEntity.ok(ApiResponse.success("Risk evaluation completed", evaluation));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionDto>> transferMoney(@Valid @RequestBody TransferRequest transferRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderUsername = authentication.getName();

        TransactionDto transactionDto = transactionService.transferMoney(senderUsername, transferRequest);

        return ResponseEntity.ok(ApiResponse.success("Money transferred successfully", transactionDto));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<TransactionDto> history = transactionService.getTransactionHistory(username);

        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully", history));
    }
}
