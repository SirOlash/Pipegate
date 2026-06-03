package com.olash.pipegate.merchant.repository;

import com.olash.pipegate.merchant.domain.MerchantVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantVerificationRepository
        extends JpaRepository<MerchantVerification, String> {

    Optional<MerchantVerification> findByMerchantEmailAndUsedFalse(
            String merchantEmail);
}
