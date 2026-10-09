package com.securepay.service;

import com.securepay.dto.TransactionDto;
import com.securepay.dto.TransferRequest;

import java.util.List;

public interface TransactionService {

    TransactionDto transferMoney(String senderUsername, TransferRequest transferRequest);

    com.securepay.dto.FraudEvaluationResult evaluateTransferRisk(String senderUsername, TransferRequest transferRequest);

    TransactionDto completeOtpVerification(String referenceCode);

    List<TransactionDto> getTransactionHistory(String username);

    List<TransactionDto> getAllTransactions();

    TransactionDto approveTransaction(Long id);

    TransactionDto blockTransaction(Long id);
}
