package com.gamerift.feature.payment.service;

import java.util.List;
import java.util.Map;

import com.gamerift.feature.payment.dto.CheckoutRequest;
import com.gamerift.feature.payment.dto.OrderResponse;
import com.gamerift.feature.payment.dto.PaymentInitResponse;
import com.gamerift.feature.payment.dto.QuoteRequest;
import com.gamerift.feature.payment.dto.QuoteResponse;

/**
 * Quote, SSLCommerz checkout, the gateway callbacks and the payment history.
 * The controller calls only this interface (never a repository).
 */
public interface ICheckoutService {

    /** Validates the plan and coupon and returns the price breakdown (nothing is saved). */
    QuoteResponse quote(QuoteRequest request);

    /**
     * Starts a payment: creates a PENDING order and asks SSLCommerz for the payment page URL.
     * Pro is NOT granted here - only the verified callback may do that.
     */
    PaymentInitResponse createPayment(String userId, CheckoutRequest request);

    /** The browser lands here after a payment. Validates, marks PAID once, returns the result path. */
    String handleSuccess(Map<String, String> params);

    /** The browser lands here after a failed payment. Returns the result path. */
    String handleFail(Map<String, String> params);

    /** The browser lands here after a cancelled payment. Returns the result path. */
    String handleCancel(Map<String, String> params);

    /** Server-to-server payment notification (backup path). Never throws: it only logs. */
    void handleIpn(Map<String, String> params);

    /** Saves a "Notify me" interest for another payment method, but never twice. */
    void notifyMe(String userId, String method);

    /** My orders, newest first. */
    List<OrderResponse> getMyOrders(String userId);

    /** One of my orders (the receipt). Throws 404 when it is not mine. */
    OrderResponse getOrder(String userId, String orderId);
}
