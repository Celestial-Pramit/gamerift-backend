package com.gamerift.feature.payment.dto;

import java.time.Instant;

import com.gamerift.feature.payment.model.OrderStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One order as the "Payment history" table and the receipt render it. */
@Getter
@Setter
@NoArgsConstructor
public class OrderResponse {

    private String id;
    private String planId;
    private String planName;
    private int days;
    private int priceBdt;
    private String code;
    private int discount;
    private int total;
    private String method;
    private OrderStatus status;
    private Instant createdAt;
}