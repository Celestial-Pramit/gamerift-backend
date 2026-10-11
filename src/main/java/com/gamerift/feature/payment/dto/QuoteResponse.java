package com.gamerift.feature.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The numbers of the checkout summary for one plan + optional coupon. */
@Getter
@Setter
@NoArgsConstructor
public class QuoteResponse {

    private String planId;
    private String planName;
    private int subtotal;
    private String code;
    private int discount;
    private int total;
}