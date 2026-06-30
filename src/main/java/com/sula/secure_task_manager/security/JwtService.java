package com.sula.secure_task_manager.security;

import com.sula.secure_task_manager.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String generateAccessToken(User user) {
        return buildToken(user, expirationMs, "access", true);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs, "refresh", false);
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return extractClaims(token).getId();
    }

    public String extractTokenType(String token) {
        Object tokenType = extractClaims(token).get("token_type");
        return tokenType == null ? null : tokenType.toString();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token) && "access".equals(extractTokenType(token));
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token) && "refresh".equals(extractTokenType(token));
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(User user, long expirationMs, String tokenType, boolean includeRole) {
        Instant now = Instant.now();

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("token_type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)));

        if (includeRole) {
            builder.claim("role", user.getRole().name());
        }

        return builder.signWith(signingKey()).compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
