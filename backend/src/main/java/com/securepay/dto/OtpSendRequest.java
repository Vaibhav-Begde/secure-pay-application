package com.securepay.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpSendRequest {

    @NotBlank(message = "Transaction reference code is required")
    private String referenceCode;

    public OtpSendRequest() {
    }

    public OtpSendRequest(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public static OtpSendRequestBuilder builder() {
        return new OtpSendRequestBuilder();
    }

    public static class OtpSendRequestBuilder {
        private String referenceCode;

        public OtpSendRequestBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }

        public OtpSendRequest build() {
            return new OtpSendRequest(referenceCode);
        }
    }
}
