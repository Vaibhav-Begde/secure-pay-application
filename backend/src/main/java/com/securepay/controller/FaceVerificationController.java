package com.securepay.controller;

import com.securepay.dto.ApiResponse;
import com.securepay.dto.FaceVerificationRequest;
import com.securepay.dto.TransactionDto;
import com.securepay.service.FaceVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/face-verification")
public class FaceVerificationController {

    private final FaceVerificationService faceVerificationService;

    @Autowired
    public FaceVerificationController(FaceVerificationService faceVerificationService) {
        this.faceVerificationService = faceVerificationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TransactionDto>> verifyFace(@Valid @RequestBody FaceVerificationRequest request) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        TransactionDto result = faceVerificationService.verifyFace(username, request.getReferenceCode(), request.getFaceImageBase64());
        return ResponseEntity.ok(ApiResponse.success("Face verification completed successfully", result));
    }

    @PostMapping("/configure")
    public ResponseEntity<ApiResponse<String>> configureFace(@Valid @RequestBody com.securepay.dto.FaceConfigurationRequest request) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        faceVerificationService.configureFace(username, request.getFaceImageBase64());
        return ResponseEntity.ok(ApiResponse.success("Face image configured successfully", null));
    }
}
