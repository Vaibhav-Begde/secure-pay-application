package com.securepay.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class WalletDto {

    private Long id;
    private Long userId;
    private String username;
    private BigDecimal balance;
    private String currency;
    private Instant updatedAt;

    public WalletDto() {
    }

    public WalletDto(Long id, Long userId, String username, BigDecimal balance, String currency, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static WalletDtoBuilder builder() {
        return new WalletDtoBuilder();
    }

    public static class WalletDtoBuilder {
        private Long id;
        private Long userId;
        private String username;
        private BigDecimal balance;
        private String currency;
        private Instant updatedAt;

        public WalletDtoBuilder id(Long id) { this.id = id; return this; }
        public WalletDtoBuilder userId(Long userId) { this.userId = userId; return this; }
        public WalletDtoBuilder username(String username) { this.username = username; return this; }
        public WalletDtoBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public WalletDtoBuilder currency(String currency) { this.currency = currency; return this; }
        public WalletDtoBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public WalletDto build() {
            return new WalletDto(id, userId, username, balance, currency, updatedAt);
        }
    }
}
