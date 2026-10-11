package com.gamerift.feature.payment.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/payment/checkout. code is optional, method defaults to bKash. */
@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Plan is required")
    private String planId;

    private String code;
    private String method;
}