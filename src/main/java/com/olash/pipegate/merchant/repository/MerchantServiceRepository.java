package com.olash.pipegate.merchant.repository;

import com.olash.pipegate.merchant.domain.MerchantService;
import com.olash.pipegate.merchant.domain.ServiceStatus;
import com.olash.pipegate.merchant.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantServiceRepository
        extends JpaRepository<MerchantService, String> {

    List<MerchantService> findByMerchantId(String merchantId);

    Optional<MerchantService> findByMerchantIdAndServiceType(
            String merchantId,
            ServiceType serviceType);

    boolean existsByMerchantIdAndServiceType(
            String merchantId,
            ServiceType serviceType);

    List<MerchantService> findByStatus(ServiceStatus status);
}