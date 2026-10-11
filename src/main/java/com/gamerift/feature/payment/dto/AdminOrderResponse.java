package com.gamerift.feature.payment.dto;

import java.time.Instant;

import com.gamerift.feature.payment.model.OrderStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the admin "Orders" screen (user = the buyer's username). */
@Getter
@Setter
@NoArgsConstructor
public class AdminOrderResponse {

    private String id;
    private String user;
    private String plan;
    private int amount;
    private OrderStatus status;
    private Instant createdAt;
}