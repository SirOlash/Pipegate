package com.olash.pipegate.merchant.repository;

import com.olash.pipegate.merchant.domain.MerchantServiceEntity;
import com.olash.pipegate.merchant.domain.ServiceStatus;
import com.olash.pipegate.merchant.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantServiceRepository
        extends JpaRepository<MerchantServiceEntity, String> {

    List<MerchantServiceEntity> findByMerchantId(String merchantId);

    Optional<MerchantServiceEntity> findByMerchantIdAndServiceType(
            String merchantId,
            ServiceType serviceType);

    boolean existsByMerchantIdAndServiceType(
            String merchantId,
            ServiceType serviceType);

    List<MerchantServiceEntity> findByStatus(ServiceStatus status);
}