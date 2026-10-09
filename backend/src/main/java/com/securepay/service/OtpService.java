package com.securepay.service;

import com.securepay.dto.OtpSendRequest;
import com.securepay.dto.OtpVerifyRequest;
import com.securepay.dto.TransactionDto;
import com.securepay.model.Transaction;

public interface OtpService {

    void generateAndSendOtp(Transaction transaction);

    TransactionDto sendOtpForReferenceCode(String username, OtpSendRequest sendRequest);

    TransactionDto verifyOtp(String username, OtpVerifyRequest verifyRequest);
}
