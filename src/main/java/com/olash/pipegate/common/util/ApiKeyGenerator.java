package com.olash.pipegate.common.util;

import com.olash.pipegate.merchant.domain.MerchantMode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {

    private static final int KEY_BYTE_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateApiKey(MerchantMode mode) {
        String prefix = mode == MerchantMode.SANDBOX
                ? "pk_sandbox_"
                : "pk_live_";
        return prefix + generateRandomString();
    }

    public String generateSecretKey(MerchantMode mode) {
        String prefix = mode == MerchantMode.SANDBOX
                ? "pk_sandbox_"
                : "pk_live_";
        return prefix + generateRandomString();
    }

    private String generateRandomString() {
        byte[] randomBytes = new byte[KEY_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public String generateMerchantCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("PG-MCH-");

        for (int i = 0; i < 6; i++) {
            int index = secureRandom.nextInt(characters.length());
            code.append(characters.charAt(index));
        }

        return code.toString();
    }
}
