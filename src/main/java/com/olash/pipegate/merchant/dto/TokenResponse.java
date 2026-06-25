package com.olash.pipegate.merchant.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresInMinutes;
}
