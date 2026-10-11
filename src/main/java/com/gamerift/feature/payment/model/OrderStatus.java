package com.gamerift.feature.payment.model;

/**
 * Lifecycle of an order.
 *
 * The gateway flow is: checkout starts a PENDING order, then the gateway callback moves it
 * to PAID, FAILED or CANCELLED. A valid late payment notification may still turn a FAILED or
 * CANCELLED order into PAID (the user can retry on the gateway page). PAID never changes back.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED
}