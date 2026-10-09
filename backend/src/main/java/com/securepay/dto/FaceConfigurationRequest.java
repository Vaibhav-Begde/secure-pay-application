package com.securepay.dto;

import jakarta.validation.constraints.NotBlank;

public class FaceConfigurationRequest {

    @NotBlank(message = "Face image is required")
    private String faceImageBase64;

    public FaceConfigurationRequest() {
    }

    public FaceConfigurationRequest(String faceImageBase64) {
        this.faceImageBase64 = faceImageBase64;
    }

    public String getFaceImageBase64() { return faceImageBase64; }
    public void setFaceImageBase64(String faceImageBase64) { this.faceImageBase64 = faceImageBase64; }
}
