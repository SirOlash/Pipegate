package com.olash.pipegate.merchant.dto;

import com.olash.pipegate.merchant.domain.MerchantMode;
import com.olash.pipegate.merchant.domain.MerchantStatus;
import com.olash.pipegate.merchant.domain.ServiceStatus;
import com.olash.pipegate.merchant.domain.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class LoginResponse {

    private String merchantId;
    private String merchantCode;
    private String businessName;
    private String email;
    private MerchantMode mode;
    private MerchantStatus status;
    private Map<ServiceType, ServiceStatus> services;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long accessTokenExpiresInMinutes;
}
