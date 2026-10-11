package com.gamerift.feature.payment.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.gamerift.feature.payment.dto.CheckoutRequest;
import com.gamerift.feature.payment.dto.NotifyMeRequest;
import com.gamerift.feature.payment.dto.OrderResponse;
import com.gamerift.feature.payment.dto.PaymentInitResponse;
import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.dto.QuoteRequest;
import com.gamerift.feature.payment.dto.QuoteResponse;
import com.gamerift.feature.payment.service.ICheckoutService;
import com.gamerift.feature.payment.service.IPlanService;
import com.gamerift.shared.dto.MessageResponse;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Go Pro flow: plans, quote, SSLCommerz checkout and the payment history.
 *
 * The three callback URLs below are called by SSLCommerz, not by our frontend, so they are
 * public (see SecurityConfig) and they answer with a redirect (303) back to a frontend page.
 * They are wrapped in try/catch on purpose: a user is mid-payment, so a code error must never
 * show them a raw 500 page.
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Pro plans, quote, SSLCommerz checkout and payment history")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private static final String FAILED_PAGE = "/app/pro/result/failed";

    private final IPlanService planService;
    private final ICheckoutService checkoutService;
    private final String frontendUrl;

    public PaymentController(IPlanService planService, ICheckoutService checkoutService,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.planService = planService;
        this.checkoutService = checkoutService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/plans")
    @Operation(summary = "Active Pro plans, cheapest first")
    public List<PlanResponse> getActivePlans() {
        return planService.getActivePlans();
    }

    @PostMapping("/quote")
    @Operation(summary = "Ask the server what the price would be (nothing is saved)")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        return checkoutService.quote(request);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a payment: creates a PENDING order and returns the gateway payment URL")
    public PaymentInitResponse checkout(@Valid @RequestBody CheckoutRequest request,
                                        @AuthenticationPrincipal AuthUser user) {
        return checkoutService.createPayment(user.getId(), request);
    }

    @GetMapping("/orders")
    @Operation(summary = "My payment history, newest first")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal AuthUser user) {
        return checkoutService.getMyOrders(user.getId());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "One receipt of mine")
    public OrderResponse getOrder(@PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        return checkoutService.getOrder(user.getId(), id);
    }

    @PostMapping("/notify-me")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save interest in another payment method (backend hook, no UI yet)")
    public MessageResponse notifyMe(@Valid @RequestBody NotifyMeRequest request,
                                    @AuthenticationPrincipal AuthUser user) {
        checkoutService.notifyMe(user.getId(), request.getMethod());
        return new MessageResponse("We will notify you about " + request.getMethod());
    }

    // ------------------------------------------------------- SSLCommerz callbacks

    @PostMapping("/sslcommerz/success")
    @Operation(summary = "SSLCommerz success callback: verify the payment and redirect the browser")
    public ResponseEntity<Void> sslCommerzSuccess(@RequestParam Map<String, String> params) {
        String path;
        try {
            path = checkoutService.handleSuccess(params);
        } catch (Exception e) {
            log.error("SSLCommerz success callback failed", e);
            path = FAILED_PAGE;
        }
        return redirectTo(path);
    }

    @PostMapping("/sslcommerz/fail")
    @Operation(summary = "SSLCommerz failure callback: mark the order and redirect the browser")
    public ResponseEntity<Void> sslCommerzFail(@RequestParam Map<String, String> params) {
        String path;
        try {
            path = checkoutService.handleFail(params);
        } catch (Exception e) {
            log.error("SSLCommerz fail callback failed", e);
            path = FAILED_PAGE;
        }
        return redirectTo(path);
    }

    @PostMapping("/sslcommerz/cancel")
    @Operation(summary = "SSLCommerz cancel callback: mark the order and redirect the browser")
    public ResponseEntity<Void> sslCommerzCancel(@RequestParam Map<String, String> params) {
        String path;
        try {
            path = checkoutService.handleCancel(params);
        } catch (Exception e) {
            log.error("SSLCommerz cancel callback failed", e);
            path = FAILED_PAGE;
        }
        return redirectTo(path);
    }

    @PostMapping("/sslcommerz/ipn")
    @Operation(summary = "SSLCommerz server-to-server notification (backup path, always 200)")
    public ResponseEntity<Void> sslCommerzIpn(@RequestParam Map<String, String> params) {
        // The service logs and swallows every error, so this always returns 200 to the gateway.
        checkoutService.handleIpn(params);
        return ResponseEntity.ok().build();
    }

    // 303 See Other: the browser switches from the gateway POST to a normal GET of our page.
    private ResponseEntity<Void> redirectTo(String path) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(frontendUrl + path))
                .build();
    }
}
