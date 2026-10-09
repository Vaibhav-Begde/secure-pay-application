package com.securepay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OtpVerifyRequest {

    @NotBlank(message = "Transaction reference code is required")
    private String referenceCode;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 digits")
    private String otpCode;

    public OtpVerifyRequest() {
    }

    public OtpVerifyRequest(String referenceCode, String otpCode) {
        this.referenceCode = referenceCode;
        this.otpCode = otpCode;
    }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public static OtpVerifyRequestBuilder builder() {
        return new OtpVerifyRequestBuilder();
    }

    public static class OtpVerifyRequestBuilder {
        private String referenceCode;
        private String otpCode;

        public OtpVerifyRequestBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public OtpVerifyRequestBuilder otpCode(String otpCode) { this.otpCode = otpCode; return this; }

        public OtpVerifyRequest build() {
            return new OtpVerifyRequest(referenceCode, otpCode);
        }
    }
}
