package com.olash.pipegate.merchant.controller;


import com.olash.pipegate.common.response.ApiResponse;
import com.olash.pipegate.merchant.domain.Merchant;
import com.olash.pipegate.merchant.dto.*;
import com.olash.pipegate.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received. email={}", request.getEmail());

        LoginResponse response = merchantService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Login successful",
                        response));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse response = merchantService.refreshAccessToken(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Access token refreshed successfully",
                        response));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {

        Merchant merchant = (Merchant) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        merchantService.logout(merchant.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Logged out successfully"));
    }
}
