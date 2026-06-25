package com.olash.pipegate.merchant.dto;

import com.olash.pipegate.merchant.domain.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServiceRequestDto {
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
}
