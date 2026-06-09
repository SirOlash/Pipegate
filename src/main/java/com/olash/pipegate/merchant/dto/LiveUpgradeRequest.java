package com.olash.pipegate.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LiveUpgradeRequest {

    @NotBlank(message = "RC number is required for live access")
    private String rcNumber;

    @NotBlank(message = "CAC number is required for live access")
    private String cacNumber;

    @NotBlank(message = "Business address is required for live access")
    private String businessAddress;

    @NotBlank(message = "State is required for live access")
    private String state;

    private String country;
}
