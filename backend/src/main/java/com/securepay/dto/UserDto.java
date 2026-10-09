package com.securepay.dto;

import com.securepay.model.Role;
import java.time.Instant;

public class UserDto {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean transactionPinSet;
    private Instant lastLoginAt;
    private String lastLoginDevice;
    private String lastLoginIp;
    private String lastLoginLocation;
    private Instant createdAt;

    public UserDto() {
    }

    public UserDto(Long id, String username, String email, Role role, boolean enabled, boolean transactionPinSet, Instant lastLoginAt, String lastLoginDevice, String lastLoginIp, String lastLoginLocation, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.transactionPinSet = transactionPinSet;
        this.lastLoginAt = lastLoginAt;
        this.lastLoginDevice = lastLoginDevice;
        this.lastLoginIp = lastLoginIp;
        this.lastLoginLocation = lastLoginLocation;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTransactionPinSet() { return transactionPinSet; }
    public void setTransactionPinSet(boolean transactionPinSet) { this.transactionPinSet = transactionPinSet; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getLastLoginDevice() { return lastLoginDevice; }
    public void setLastLoginDevice(String lastLoginDevice) { this.lastLoginDevice = lastLoginDevice; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    public String getLastLoginLocation() { return lastLoginLocation; }
    public void setLastLoginLocation(String lastLoginLocation) { this.lastLoginLocation = lastLoginLocation; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static UserDtoBuilder builder() {
        return new UserDtoBuilder();
    }

    public static class UserDtoBuilder {
        private Long id;
        private String username;
        private String email;
        private Role role;
        private boolean enabled;
        private boolean transactionPinSet;
        private Instant lastLoginAt;
        private String lastLoginDevice;
        private String lastLoginIp;
        private String lastLoginLocation;
        private Instant createdAt;

        public UserDtoBuilder id(Long id) { this.id = id; return this; }
        public UserDtoBuilder username(String username) { this.username = username; return this; }
        public UserDtoBuilder email(String email) { this.email = email; return this; }
        public UserDtoBuilder role(Role role) { this.role = role; return this; }
        public UserDtoBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public UserDtoBuilder transactionPinSet(boolean transactionPinSet) { this.transactionPinSet = transactionPinSet; return this; }
        public UserDtoBuilder lastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        public UserDtoBuilder lastLoginDevice(String lastLoginDevice) { this.lastLoginDevice = lastLoginDevice; return this; }
        public UserDtoBuilder lastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; return this; }
        public UserDtoBuilder lastLoginLocation(String lastLoginLocation) { this.lastLoginLocation = lastLoginLocation; return this; }
        public UserDtoBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public UserDto build() {
            return new UserDto(id, username, email, role, enabled, transactionPinSet, lastLoginAt, lastLoginDevice, lastLoginIp, lastLoginLocation, createdAt);
        }
    }
}
