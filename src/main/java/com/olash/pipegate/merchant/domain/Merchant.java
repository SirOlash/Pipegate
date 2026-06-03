package com.olash.pipegate.merchant.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "merchant_code", unique = true)
    private String merchantCode;

    @Column(name = "api_key", unique = true)
    private String apiKey;

    @Column(name = "secret_key_hash")
    private String secretKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private MerchantMode mode;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "allowed_ips")
    private String allowedIps;

    @Column(name = "rc_number")
    private String rcNumber;

    @Column(name = "cac_number")
    private String cacNumber;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MerchantStatus status;

    @OneToMany(mappedBy = "merchant",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<MerchantService> services = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.country == null) {
            this.country = "Nigeria";
        }
        if (this.mode == null) {
            this.mode = MerchantMode.SANDBOX;
        }
    }

    public boolean hasActiveService(ServiceType serviceType) {
        for (MerchantService service : this.services) {
            if (service.getServiceType() == serviceType &&
                    service.getStatus() == ServiceStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }


}
