package com.securepay.dto;

import com.securepay.model.Role;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean transactionPinSet;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, Long id, String username, String email, Role role, boolean transactionPinSet, String message) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.transactionPinSet = transactionPinSet;
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isTransactionPinSet() { return transactionPinSet; }
    public void setTransactionPinSet(boolean transactionPinSet) { this.transactionPinSet = transactionPinSet; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private String token;
        private String tokenType = "Bearer";
        private Long id;
        private String username;
        private String email;
        private Role role;
        private boolean transactionPinSet;
        private String message;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public AuthResponseBuilder id(Long id) { this.id = id; return this; }
        public AuthResponseBuilder username(String username) { this.username = username; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder role(Role role) { this.role = role; return this; }
        public AuthResponseBuilder transactionPinSet(boolean transactionPinSet) { this.transactionPinSet = transactionPinSet; return this; }
        public AuthResponseBuilder message(String message) { this.message = message; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, id, username, email, role, transactionPinSet, message);
        }
    }
}
