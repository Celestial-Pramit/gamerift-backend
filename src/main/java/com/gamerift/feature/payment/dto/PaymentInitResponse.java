package com.gamerift.feature.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the checkout endpoint answers after a payment session is created.
 * The frontend reads gatewayUrl and sends the browser there to pay.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentInitResponse {

    private String orderId;
    private int amount;
    private String gatewayUrl;
}
