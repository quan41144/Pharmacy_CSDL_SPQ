package com.pharmacy.medicare.dto.auth;

import java.math.BigDecimal;
import java.util.Set;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String fullName;
    private BigDecimal balance;
    private Set<String> roles;
    private String status;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, Long id,
                        String username, String fullName, BigDecimal balance,
                        Set<String> roles, String status) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.balance = balance;
        this.roles = roles;
        this.status = status;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
