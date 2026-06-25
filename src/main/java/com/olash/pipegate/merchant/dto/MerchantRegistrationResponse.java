package com.olash.pipegate.merchant.dto;

import com.olash.pipegate.merchant.domain.MerchantMode;
import com.olash.pipegate.merchant.domain.MerchantStatus;
import com.olash.pipegate.merchant.domain.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class MerchantRegistrationResponse {

    private String merchantId;
    private String merchantCode;
    private String businessName;
    private String email;
    private String phoneNumber;
    private MerchantMode mode;
    private MerchantStatus status;
    private Set<ServiceType> requestedServices;
    private String message;
    private LocalDateTime createdAt;
}
