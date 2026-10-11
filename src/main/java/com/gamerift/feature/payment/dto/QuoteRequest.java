package com.gamerift.feature.payment.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/payment/quote. code is optional ("" or null = no coupon). */
@Getter
@Setter
@NoArgsConstructor
public class QuoteRequest {

    @NotBlank(message = "Plan is required")
    private String planId;

    private String code;
}