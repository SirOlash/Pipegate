package com.olash.pipegate.merchant.service;

import com.olash.pipegate.common.exception.DuplicateEmailException;
import com.olash.pipegate.common.exception.InvalidOtpException;
import com.olash.pipegate.common.exception.MerchantNotFoundException;
import com.olash.pipegate.common.exception.UnauthorizedServiceException;
import com.olash.pipegate.common.util.ApiKeyGenerator;
import com.olash.pipegate.common.util.OtpUtil;
import com.olash.pipegate.merchant.domain.*;
import com.olash.pipegate.merchant.dto.*;
import com.olash.pipegate.merchant.repository.MerchantRepository;
import com.olash.pipegate.merchant.repository.MerchantServiceRepository;
import com.olash.pipegate.merchant.repository.MerchantVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        merchantRepository.save(merchant);

        log.info("Merchant record created. merchantCode={}, email={}",
                merchantCode, request.getEmail());

        Set<ServiceType> requestedServices = request.getRequestedServices();

        requestedServices.forEach(serviceType -> {
            MerchantServiceEntity serviceRecord =
                    MerchantServiceEntity.builder()
                    .merchant(merchant)
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

}
