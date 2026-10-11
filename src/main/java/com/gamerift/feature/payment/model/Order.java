package com.gamerift.feature.payment.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One purchase (collection "orders").
 * The id is the transaction id shown on the receipt (for example "GR-1728651234567-482").
 * planName / days / priceBdt are copied from the plan at payment time so the history
 * never changes when a plan is edited or deactivated later.
 *
 * The gateway fields (gateway, valId, bankTranId, paidAt) stay empty until the
 * SSLCommerz callback has verified the payment and moved the status to PAID.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String userId;
    private String planId;
    private String planName;
    private int days;
    private int priceBdt;
    // discount code that was applied ("") or null when none
    private String code;
    private int discount;
    private int total;
    // the method shown to the user ("SSLCommerz", or the card/bank name the gateway reports)
    private String method = "SSLCommerz";
    // which gateway created this order ("SSLCOMMERZ")
    private String gateway = "SSLCOMMERZ";
    // every new order starts PENDING and only becomes PAID after the gateway confirms
    private OrderStatus status = OrderStatus.PENDING;
    // SSLCommerz validation id, returned by the gateway and checked on our server
    private String valId;
    // the transaction id on the bank/gateway side
    private String bankTranId;
    // when the payment was confirmed as PAID
    private Instant paidAt;
    private Instant createdAt = Instant.now();
}