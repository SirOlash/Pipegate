package com.olash.pipegate.merchant.dto;

import com.olash.pipegate.merchant.domain.MerchantMode;
import com.olash.pipegate.merchant.domain.ServiceStatus;
import com.olash.pipegate.merchant.domain.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class MerchantActivationResponse {

    private String merchantId;
    private String merchantCode;
    private String businessName;
    private String email;
    private String apiKey;
    private String secretKey;
    private MerchantMode mode;
    private Map<ServiceType, ServiceStatus> services;
    private String message;
}
