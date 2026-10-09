package com.securepay.service;

import com.securepay.dto.WalletDto;
import com.securepay.model.User;
import com.securepay.model.Wallet;

import java.math.BigDecimal;

public interface WalletService {

    WalletDto getWalletByUsername(String username);

    Wallet createInitialWalletForUser(User user, BigDecimal initialBalance);
}
