package com.securepay.service;

import com.securepay.dto.TransactionDto;

public interface FaceVerificationService {
    TransactionDto verifyFace(String username, String referenceCode, String base64Image);
    void configureFace(String username, String base64Image);
}
