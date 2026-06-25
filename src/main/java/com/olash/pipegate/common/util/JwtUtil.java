package com.olash.pipegate.common.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Getter
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtUtil(
            @Value("${pipegate.jwt.secret}") String secret,
            @Value("${pipegate.jwt.access-token-expiry-minutes}")
            long accessTokenExpiryMinutes,
            @Value("${pipegate.jwt.refresh-token-expiry-days}")
            long refreshTokenExpiryDays) {

        byte[] keyBytes = HexFormat.of().parseHex(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    public String generateAccessToken(String merchantId,
                                      String merchantCode,
                                      String email) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() +
                (accessTokenExpiryMinutes * 60 * 1000));

        return Jwts.builder()
                .subject(merchantId)
                .claim("merchantCode", merchantCode)
                .claim("email", email)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateRawRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

//    public long getRefreshTokenExpiryDays() {
//        return refreshTokenExpiryDays;
//    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractMerchantId(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("Invalid JWT token. error={}", e.getMessage());
            return false;
        }
    }
}
