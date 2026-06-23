package com.olash.pipegate.merchant.controller;


import com.olash.pipegate.common.response.ApiResponse;
import com.olash.pipegate.merchant.dto.*;
import com.olash.pipegate.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantRegistrationResponse>> register(
            @Valid @RequestBody MerchantRegistrationRequest request) {

        log.info("Registration request received. email={}",
                request.getEmail());

        MerchantRegistrationResponse response =
                merchantService.registerMerchant(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. " +
                                "Please check your email for your OTP.",
                        response));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<MerchantActivationResponse>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {

        log.info("Email verification request received. email={}",
                request.getEmail());

        MerchantActivationResponse response =
                merchantService.verifyEmail(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Account activated successfully.",
                        response));
    }

    @PostMapping("/services/request")
    public ResponseEntity<ApiResponse<String>> requestService(
            @Valid @RequestBody ServiceRequestDto request,
            @RequestHeader("X-Merchant-Id") String merchantId) {

        log.info("Service request received. merchantId={}, serviceType={}",
                merchantId, request.getServiceType());

        String message = merchantService
                .requestAdditionalService(merchantId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(message));
    }

    @PostMapping("/upgrade/live")
    public ResponseEntity<ApiResponse<String>> requestLiveUpgrade(
            @Valid @RequestBody LiveUpgradeRequest request,
            @RequestHeader("X-Merchant-Id") String merchantId) {

        log.info("Live upgrade request received. merchantId={}",
                merchantId);

        String message = merchantService
                .requestLiveUpgrade(merchantId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(message));
    }
}
