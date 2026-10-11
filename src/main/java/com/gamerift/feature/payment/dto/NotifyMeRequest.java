package com.gamerift.feature.payment.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/payment/notify-me: which payment method the user wants to hear about. */
@Getter
@Setter
@NoArgsConstructor
public class NotifyMeRequest {

    @NotBlank(message = "Payment method is required")
    private String method;
}
