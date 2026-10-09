package com.securepay.dto;

import jakarta.validation.constraints.NotBlank;

public class FaceVerificationRequest {

    @NotBlank(message = "Reference code is required")
    private String referenceCode;

    @NotBlank(message = "Face image is required")
    private String faceImageBase64;

    public FaceVerificationRequest() {
    }

    public FaceVerificationRequest(String referenceCode, String faceImageBase64) {
        this.referenceCode = referenceCode;
        this.faceImageBase64 = faceImageBase64;
    }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getFaceImageBase64() { return faceImageBase64; }
    public void setFaceImageBase64(String faceImageBase64) { this.faceImageBase64 = faceImageBase64; }
}
