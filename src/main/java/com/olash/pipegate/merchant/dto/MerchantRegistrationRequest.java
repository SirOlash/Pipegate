package com.olash.pipegate.merchant.dto;

import com.olash.pipegate.merchant.domain.MerchantMode;
import com.olash.pipegate.merchant.domain.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
public class MerchantRegistrationRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+234|0)[789][01]\\d{8}$",
            message = "Must be a valid Nigerian phone number"
    )
    private String phoneNumber;

    @NotEmpty(message = "At least one service must be selected")
    private Set<ServiceType> requestedServices;

    private String webhookUrl;
}
