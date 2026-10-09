package com.securepay.service;

import com.securepay.dto.WalletDto;
import com.securepay.exception.ResourceNotFoundException;
import com.securepay.model.User;
import com.securepay.model.Wallet;
import com.securepay.repository.UserRepository;
import com.securepay.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("1000.00");

    @Autowired
    public WalletServiceImpl(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public WalletDto getWalletByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> createInitialWalletForUser(user, DEFAULT_INITIAL_BALANCE));

        return mapToWalletDto(wallet);
    }

    @Override
    @Transactional
    public Wallet createInitialWalletForUser(User user, BigDecimal initialBalance) {
        BigDecimal balance = (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) >= 0)
                ? initialBalance
                : DEFAULT_INITIAL_BALANCE;

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(balance)
                .currency("INR")
                .build();

        return walletRepository.save(wallet);
    }

    private WalletDto mapToWalletDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .username(wallet.getUser().getUsername())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
