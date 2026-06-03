package com.olash.pipegate.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final int OTP_LENGTH = 6;
    private static final String DIGITS = "0123456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = secureRandom.nextInt(DIGITS.length());
            otp.append(DIGITS.charAt(index));
        }

        return otp.toString();
    }

    public String hashOtp(String otp) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    otp.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available",e);
        }
    }

    public boolean verifyOtp(String rawOtp, String storedHash) {
        String hashOfProvided = hashOtp(rawOtp);
        return hashOfProvided.equals(storedHash);
    }
}
