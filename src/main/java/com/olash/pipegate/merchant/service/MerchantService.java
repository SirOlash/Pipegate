package com.olash.pipegate.merchant.service;

import com.olash.pipegate.common.exception.*;
import com.olash.pipegate.common.util.ApiKeyGenerator;
import com.olash.pipegate.common.util.JwtUtil;
import com.olash.pipegate.common.util.OtpUtil;
import com.olash.pipegate.merchant.domain.*;
import com.olash.pipegate.merchant.dto.*;
import com.olash.pipegate.merchant.repository.MerchantRepository;
import com.olash.pipegate.merchant.repository.MerchantServiceRepository;
import com.olash.pipegate.merchant.repository.MerchantVerificationRepository;
import com.olash.pipegate.merchant.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantServiceRepository merchantServiceRepository;
    private final MerchantVerificationRepository verificationRepository;
    private final EmailService emailService;
    private final ApiKeyGenerator apiKeyGenerator;
    private final OtpUtil otpUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional
    public MerchantRegistrationResponse registerMerchant(
            MerchantRegistrationRequest request) {

        log.info("Merchant registration attempt. email={}",
                request.getEmail());

        if (merchantRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed, email already exists. email={}",
                    request.getEmail());
            throw new DuplicateEmailException(
                    "A merchant with this email already exists");
        }

        String merchantCode = apiKeyGenerator.generateMerchantCode();

        Merchant merchant = Merchant.builder()
                .businessName(request.getBusinessName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .merchantCode(merchantCode)
                .mode(MerchantMode.SANDBOX)
                .webhookUrl(request.getWebhookUrl())
                .status(MerchantStatus.PENDING_VERIFICATION)
                .build();

        merchant = merchantRepository.save(merchant);

        merchantRepository.flush();
        log.info("CreatedAt = {}", merchant.getCreatedAt());

        log.info("Merchant record created. merchantCode={}, email={}",
                merchantCode, request.getEmail());

        Set<ServiceType> requestedServices = request.getRequestedServices();

        Merchant savedMerchant = merchant;
        requestedServices.forEach(serviceType -> {
            MerchantServiceEntity serviceRecord =
                    MerchantServiceEntity.builder()
                            .merchant(savedMerchant)
                            .serviceType(serviceType)
                            .status(ServiceStatus.ACTIVE)
                            .requestedAt(LocalDateTime.now())
                            .reviewedBy("SYSTEM")
                            .reviewedAt(LocalDateTime.now())
                            .build();

            merchantServiceRepository.save(serviceRecord);
        });

        log.info("Services created for merchant. merchantCode={}, services={}",
                merchantCode, requestedServices);

        String otp = otpUtil.generateOtp();
        String otpHash = otpUtil.hashOtp(otp);

        MerchantVerification verification = MerchantVerification.builder()
                .merchantEmail(request.getEmail())
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();


        verificationRepository.save(verification);

        emailService.sendOtpEmail(
                request.getEmail(),
                request.getBusinessName(),
                otp);

        log.info("OTP sent successfully. email={}", request.getEmail());

        return MerchantRegistrationResponse.builder()
                .merchantId(merchant.getId())
                .merchantCode(merchantCode)
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .phoneNumber(merchant.getPhoneNumber())
                .mode(merchant.getMode())
                .status(merchant.getStatus())
                .requestedServices(requestedServices)
                .message("Registration successful. " +
                        "Please check your email for your OTP.")
                .createdAt(merchant.getCreatedAt())
                .build();
    }


    @Transactional
    public MerchantActivationResponse verifyEmail(
            EmailVerificationRequest request) {

        log.info("Email verification attempt. email={}", request.getEmail());

        Merchant merchant = merchantRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new MerchantNotFoundException(
                        "No merchant found with email: " + request.getEmail()));

        if (merchant.getStatus() == MerchantStatus.ACTIVE) {
            log.warn("Verification attempted on already active account. email={}",
                    request.getEmail());
            throw new InvalidOtpException(
                    "This account is already verified");
        }

        MerchantVerification verification = verificationRepository
                .findByMerchantEmailAndUsedFalse(request.getEmail())
                .orElseThrow(() -> new InvalidOtpException(
                        "No active OTP found for this email. " +
                                "Please request a new one"));

        if (verification.isExpired()) {
            log.warn("Expired OTP used. email={}", request.getEmail());
            throw new InvalidOtpException(
                    "OTP has expired. Please request a new one");
        }

        if (!otpUtil.verifyOtp(request.getOtp(), verification.getOtpHash())) {
            log.warn("Invalid OTP provided. email={}", request.getEmail());
            throw new InvalidOtpException("Invalid OTP");
        }

        String rawApiKey = apiKeyGenerator.generateApiKey(merchant.getMode());
        String rawSecretKey = apiKeyGenerator.generateSecretKey(merchant.getMode());

        merchant.setApiKey(rawApiKey);
        merchant.setSecretKeyHash(passwordEncoder.encode(rawSecretKey));
        merchant.setStatus(MerchantStatus.ACTIVE);
        merchantRepository.save(merchant);

        verification.setUsed(true);
        verificationRepository.save(verification);

        log.info("Merchant activated successfully. merchantCode={}, email={}",
                merchant.getMerchantCode(), merchant.getEmail());

        emailService.sendWelcomeEmail(
                merchant.getEmail(),
                merchant.getBusinessName(),
                merchant.getMerchantCode());

        Map<ServiceType, ServiceStatus> servicesMap = merchantServiceRepository
                .findByMerchantId(merchant.getId())
                .stream()
                .collect(Collectors.toMap(
                        MerchantServiceEntity::getServiceType,
                        MerchantServiceEntity::getStatus));

        return MerchantActivationResponse.builder()
                .merchantId(merchant.getId())
                .merchantCode(merchant.getMerchantCode())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .apiKey(rawApiKey)
                .secretKey(rawSecretKey)
                .mode(merchant.getMode())
                .services(servicesMap)
                .message("Account activated successfully. " +
                        "Store your secret key safely. " +
                        "It will not be shown again.")
                .build();
    }

    @Transactional
    public String requestAdditionalService(String merchantId,
                                           ServiceRequestDto request) {

        log.info("Service request. merchantId={}, serviceType={}",
                merchantId, request.getServiceType());

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant not found"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new UnauthorizedServiceException(
                    "Only active merchants can request additional services");
        }

        if (merchantServiceRepository.existsByMerchantIdAndServiceType(
                merchantId, request.getServiceType())) {
            throw new DuplicateEmailException(
                    "You already have access to " +
                            request.getServiceType().name() +
                            " or have a pending request for it");
        }

        MerchantServiceEntity newService = MerchantServiceEntity.builder()
                .merchant(merchant)
                .serviceType(request.getServiceType())
                .status(ServiceStatus.PENDING_APPROVAL)
                .requestedAt(LocalDateTime.now())
                .build();

        merchantServiceRepository.save(newService);

        log.info("Service request created. merchantId={}, serviceType={}, status=PENDING_APPROVAL",
                merchantId, request.getServiceType());

        return "Your request for " +
                request.getServiceType().name() +
                " has been submitted and is pending approval";
    }

    @Transactional
    public String requestLiveUpgrade(String merchantId,
                                     LiveUpgradeRequest request) {

        log.info("Live upgrade request. merchantId={}", merchantId);

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant not found"));

        if (merchant.getMode() == MerchantMode.LIVE) {
            throw new UnauthorizedServiceException(
                    "Your account is already in live mode");
        }

        if (merchant.getStatus() == MerchantStatus.PENDING_LIVE_APPROVAL) {
            throw new UnauthorizedServiceException(
                    "You already have a pending live upgrade request");
        }

        merchant.setRcNumber(request.getRcNumber());
        merchant.setCacNumber(request.getCacNumber());
        merchant.setBusinessAddress(request.getBusinessAddress());
        merchant.setState(request.getState());
        merchant.setCountry(
                request.getCountry() != null ? request.getCountry() : "Nigeria");
        merchant.setStatus(MerchantStatus.PENDING_LIVE_APPROVAL);

        merchantRepository.save(merchant);

        log.info("Live upgrade request submitted. merchantId={}", merchantId);

        return "Your live upgrade request has been submitted. " +
                "Our team will review and respond within 2-3 business days.";
    }

    public LoginResponse login(LoginRequest request) {

        log.info("Login attempt. email={}", request.getEmail());

        Merchant merchant = merchantRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"));

        if (merchant.getStatus() == MerchantStatus.PENDING_VERIFICATION) {
            log.warn("Login attempt on unverified account. email={}",
                    request.getEmail());
            throw new AccountNotVerifiedException(
                    "Please verify your email before logging in");
        }

        if (merchant.getStatus() == MerchantStatus.SUSPENDED) {
            log.warn("Login attempt on suspended account. email={}",
                    request.getEmail());
            throw new InvalidCredentialsException(
                    "Your account has been suspended. " +
                            "Please contact support");
        }

        if (!passwordEncoder.matches(
                request.getPassword(), merchant.getPasswordHash())) {
            log.warn("Invalid password attempt. email={}", request.getEmail());
            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getEmail());

        String rawRefreshToken = jwtUtil.generateRawRefreshToken();
        String refreshTokenHash = jwtUtil.hashRefreshToken(rawRefreshToken);

        refreshTokenRepository.revokeAllMerchantTokens(merchant.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .merchant(merchant)
                .tokenHash(refreshTokenHash)
                .expiresAt(LocalDateTime.now()
                        .plusDays(jwtUtil.getRefreshTokenExpiryDays()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        Map<ServiceType, ServiceStatus> servicesMap =
                new HashMap<>();

        List<MerchantServiceEntity> serviceList =
                merchantServiceRepository
                        .findByMerchantId(merchant.getId());

        for (MerchantServiceEntity s : serviceList) {
            servicesMap.put(s.getServiceType(), s.getStatus());
        }

        log.info("Login successful. merchantCode={}",
                merchant.getMerchantCode());

        return LoginResponse.builder()
                .merchantId(merchant.getId())
                .merchantCode(merchant.getMerchantCode())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .mode(merchant.getMode())
                .status(merchant.getStatus())
                .services(servicesMap)
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresInMinutes(15)
                .build();
    }

    @Transactional
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {

        log.info("Token refresh attempt");

        String tokenHash = jwtUtil.hashRefreshToken(
                request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException(
                        "Invalid refresh token"));

        if (!refreshToken.isValid()) {
            log.warn("Expired or revoked refresh token used. merchantId={}",
                    refreshToken.getMerchant().getId());
            throw new InvalidTokenException(
                    "Refresh token has expired or been revoked. " +
                            "Please log in again");
        }

        Merchant merchant = refreshToken.getMerchant();
        String newAccessToken = jwtUtil.generateAccessToken(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getEmail());

        log.info("Access token refreshed. merchantCode={}",
                merchant.getMerchantCode());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresInMinutes(15)
                .build();
    }

    @Transactional
    public void logout(String merchantId) {

        log.info("Logout request. merchantId={}", merchantId);

        refreshTokenRepository.revokeAllMerchantTokens(merchantId);

        log.info("Merchant logged out, all tokens revoked. merchantId={}",
                merchantId);
    }
}
