package com.gamerift.feature.payment.gateway;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The answer we get back from the SSLCommerz "order validation" API.
 * It is a plain data holder (DTO) - the gateway client fills it from the JSON response.
 *
 * status values we care about: VALID (successful) and VALIDATED (successful, checked more than once).
 * Anything else means the payment must NOT be trusted.
 */
@Getter
@Setter
@NoArgsConstructor
public class SslCommerzValidation {

    private String status;
    private String tranId;
    private String amount;
    private String currency;
    private String bankTranId;
    private String cardType;
}
