package com.pharmacy.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms:86400000}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String username, String role, Long employeeId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("employeeId", employeeId);
        return createToken(claims, username, accessTokenExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshTokenExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        Object role = getClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public Long getEmployeeIdFromToken(String token) {
        Object id = getClaims(token).get("employeeId");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
