package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.FraudRuleDto;
import com.securepay.service.FraudDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/fraud-rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudRuleController {

    private final FraudDetectionService fraudDetectionService;

    @Autowired
    public AdminFraudRuleController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FraudRuleDto>>> getAllRules() {
        List<FraudRuleDto> rules = fraudDetectionService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success("Fraud rules retrieved successfully", rules));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FraudRuleDto>> createRule(@RequestBody FraudRuleDto dto) {
        FraudRuleDto created = fraudDetectionService.createRule(dto);
        return ResponseEntity.ok(ApiResponse.success("Fraud rule created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FraudRuleDto>> updateRule(@PathVariable Long id, @RequestBody FraudRuleDto dto) {
        FraudRuleDto updated = fraudDetectionService.updateRule(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Fraud rule updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        fraudDetectionService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Fraud rule deleted successfully", null));
    }
}
