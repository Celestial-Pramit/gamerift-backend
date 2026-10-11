package com.gamerift.feature.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.payment.dto.CheckoutRequest;
import com.gamerift.feature.payment.dto.OrderResponse;
import com.gamerift.feature.payment.dto.PaymentInitResponse;
import com.gamerift.feature.payment.dto.QuoteRequest;
import com.gamerift.feature.payment.dto.QuoteResponse;
import com.gamerift.feature.payment.gateway.SslCommerzClient;
import com.gamerift.feature.payment.gateway.SslCommerzValidation;
import com.gamerift.feature.payment.model.Coupon;
import com.gamerift.feature.payment.model.CouponType;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.gamerift.feature.payment.model.PaymentInterest;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.CouponRepository;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.payment.repository.PaymentInterestRepository;
import com.gamerift.feature.payment.repository.PlanRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Quote + SSLCommerz checkout (layer: service).
 *
 * The important rules:
 *  - the price is always computed on the server from the plan and a validated coupon,
 *  - a new order starts as PENDING and only becomes PAID after SSLCommerz confirms,
 *  - the callback re-validates the transaction id, amount and currency with SSLCommerz,
 *  - the PENDING -> PAID move is a single atomic database update, so a repeat callback
 *    (browser callback plus IPN) can never grant Pro twice,
 *  - a coupon is burned with one atomic update only when the order really becomes PAID.
 */
@Service
public class CheckoutServiceImpl implements ICheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    private final PlanRepository planRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final PaymentInterestRepository paymentInterestRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;
    private final SslCommerzClient sslCommerzClient;
    private final MongoTemplate mongoTemplate;

    public CheckoutServiceImpl(PlanRepository planRepository, CouponRepository couponRepository,
                               OrderRepository orderRepository, PaymentInterestRepository paymentInterestRepository,
                               UserRepository userRepository, INotificationService notificationService,
                               SslCommerzClient sslCommerzClient, MongoTemplate mongoTemplate) {
        this.planRepository = planRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.paymentInterestRepository = paymentInterestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.sslCommerzClient = sslCommerzClient;
        this.mongoTemplate = mongoTemplate;
    }

    // ------------------------------------------------------------------ quote

    @Override
    public QuoteResponse quote(QuoteRequest request) {
        Plan plan = findActivePlan(request.getPlanId());
        Coupon coupon = requireValidCoupon(request.getCode());
        PriceBreakdown breakdown = breakdown(plan.getPriceBdt(), coupon);

        QuoteResponse r = new QuoteResponse();
        r.setPlanId(plan.getId());
        r.setPlanName(plan.getName());
        r.setSubtotal(plan.getPriceBdt());
        r.setCode(coupon == null ? null : coupon.getCode());
        r.setDiscount(breakdown.discount);
        r.setTotal(breakdown.total);
        return r;
    }

    // --------------------------------------------------------------- checkout

    @Override
    public PaymentInitResponse createPayment(String userId, CheckoutRequest request) {
        // 1. the buyer must exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 2. banned accounts cannot buy (fail before anything is created)
        if (user.isBanned()) {
            throw new ForbiddenException("Your account is banned");
        }

        // 3. validate plan + coupon and compute the price on the server
        Plan plan = findActivePlan(request.getPlanId());
        Coupon coupon = requireValidCoupon(request.getCode());
        PriceBreakdown breakdown = breakdown(plan.getPriceBdt(), coupon);

        // 4. build a PENDING order (Pro is granted only after the gateway confirms)
        Order order = new Order();
        order.setId(uniqueOrderId());
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setPlanName(plan.getName());
        order.setDays(plan.getDays());
        order.setPriceBdt(plan.getPriceBdt());
        order.setCode(coupon == null ? null : coupon.getCode());
        order.setDiscount(breakdown.discount);
        order.setTotal(breakdown.total);
        order.setMethod("SSLCommerz");
        order.setGateway("SSLCOMMERZ");
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepository.save(order);

        // 5. ask SSLCommerz for the hosted payment page and hand the URL to the frontend
        String gatewayUrl = sslCommerzClient.createSession(saved, user);
        PaymentInitResponse response = new PaymentInitResponse();
        response.setOrderId(saved.getId());
        response.setAmount(saved.getTotal());
        response.setGatewayUrl(gatewayUrl);
        return response;
    }

    // -------------------------------------------------------------- callbacks

    @Override
    public String handleSuccess(Map<String, String> params) {
        String tranId = params == null ? null : params.get("tran_id");
        String valId = params == null ? null : params.get("val_id");
        if (tranId == null || valId == null) {
            // Missing data must never break the user's page: send them to the failed screen.
            log.warn("SSLCommerz success callback without tran_id or val_id");
            return failedPath(tranId);
        }

        Order order = orderRepository.findById(tranId).orElse(null);
        if (order == null) {
            log.warn("SSLCommerz success callback for unknown order {}", tranId);
            return failedPath(tranId);
        }

        // already paid (for example the IPN arrived first): just show the receipt again
        if (order.getStatus() == OrderStatus.PAID) {
            return successPath(order.getId());
        }

        SslCommerzValidation validation = sslCommerzClient.validate(valId);
        if (!isValidPayment(validation, order)) {
            log.warn("SSLCommerz validation rejected for order {} (status={}, amount={})",
                    order.getId(), validation.getStatus(), validation.getAmount());
            return failedPath(order.getId());
        }

        // if another callback already activated it between our check and the update, still show success
        activatePaid(order, validation, valId, params.get("card_type"));
        return successPath(order.getId());
    }

    @Override
    public String handleFail(Map<String, String> params) {
        String tranId = params == null ? null : params.get("tran_id");
        markNotPaid(tranId, OrderStatus.FAILED);
        return failedPath(tranId);
    }

    @Override
    public String handleCancel(Map<String, String> params) {
        String tranId = params == null ? null : params.get("tran_id");
        markNotPaid(tranId, OrderStatus.CANCELLED);
        return cancelledPath(tranId);
    }

    @Override
    public void handleIpn(Map<String, String> params) {
        // The IPN is the backup path. It must always answer 200, so we catch everything here.
        try {
            String tranId = params == null ? null : params.get("tran_id");
            String valId = params == null ? null : params.get("val_id");
            if (tranId == null || valId == null) {
                log.warn("SSLCommerz IPN without tran_id or val_id");
                return;
            }
            Order order = orderRepository.findById(tranId).orElse(null);
            if (order == null) {
                log.warn("SSLCommerz IPN for unknown order {}", tranId);
                return;
            }
            if (order.getStatus() == OrderStatus.PAID) {
                return; // already done
            }
            SslCommerzValidation validation = sslCommerzClient.validate(valId);
            if (!isValidPayment(validation, order)) {
                log.warn("SSLCommerz IPN validation rejected for order {}", order.getId());
                return;
            }
            activatePaid(order, validation, valId, params.get("card_type"));
        } catch (Exception e) {
            log.error("SSLCommerz IPN handling failed", e);
        }
    }

    // --------------------------------------------------------------- notify me

    @Override
    public void notifyMe(String userId, String method) {
        String clean = method == null ? "" : method.trim();
        if (clean.isEmpty()) {
            throw new BadRequestException("Payment method is required");
        }
        // no duplicates: the same user + method is stored only once
        if (paymentInterestRepository.existsByUserIdAndMethod(userId, clean)) {
            return;
        }
        PaymentInterest interest = new PaymentInterest();
        interest.setUserId(userId);
        interest.setMethod(clean);
        paymentInterestRepository.save(interest);
    }

    // ----------------------------------------------------------------- history

    @Override
    public List<OrderResponse> getMyOrders(String userId) {
        List<Order> orders = new ArrayList<>(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(PaymentMapper.toOrderResponse(order));
        }
        return result;
    }

    @Override
    public OrderResponse getOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));
        return PaymentMapper.toOrderResponse(order);
    }

    // ------------------------------------------------------------ paid activate

    /**
     * Moves the order to PAID exactly once, using one atomic database update.
     * Only when this call wins the update does it extend Pro and burn the coupon.
     * Returns true when THIS call did the activation.
     */
    private boolean activatePaid(Order order, SslCommerzValidation validation, String valId, String cardType) {
        Query query = Query.query(Criteria.where("_id").is(order.getId())
                .and("status").in(OrderStatus.PENDING, OrderStatus.FAILED, OrderStatus.CANCELLED));
        Update update = new Update()
                .set("status", OrderStatus.PAID)
                .set("valId", valId)
                .set("bankTranId", validation.getBankTranId())
                .set("method", methodName(cardType, valId))
                .set("paidAt", Instant.now());

        Order won = mongoTemplate.findAndModify(query, update, Order.class);
        if (won == null) {
            // another callback / IPN already activated it
            return false;
        }

        // give the buyer Pro for the number of days the plan promised
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            Instant now = Instant.now();
            Instant base = user.getProUntil() != null && user.getProUntil().isAfter(now) ? user.getProUntil() : now;
            user.setPro(true);
            user.setProUntil(base.plusSeconds(order.getDays() * 86400L));
            userRepository.save(user);

            notificationService.send(user.getId(), "pay",
                    "Pro activated: receipt " + order.getId(), "/app/settings");
        }

        burnCouponOnce(order.getCode());
        return true;
    }

    // Moves a PENDING order to FAILED or CANCELLED with one atomic update (no overwrite of PAID).
    private void markNotPaid(String tranId, OrderStatus status) {
        if (tranId == null) {
            return;
        }
        Query query = Query.query(Criteria.where("_id").is(tranId).and("status").is(OrderStatus.PENDING));
        mongoTemplate.updateFirst(query, new Update().set("status", status), Order.class);
    }

    /** Burns one use of a coupon, but only if it still has uses left (single atomic update). */
    private void burnCouponOnce(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            return;
        }
        Query query = Query.query(Criteria.where("_id").is(coupon.getId()).and("usedCount").lt(coupon.getMaxUses()));
        Coupon won = mongoTemplate.findAndModify(query, new Update().inc("usedCount", 1), Coupon.class);
        if (won == null) {
            // the coupon ran out while this payment was in progress: grant Pro but do not burn it
            log.warn("Coupon {} had no uses left at activation time", code);
        }
    }

    /** Confirms that SSLCommerz really accepted this exact order. */
    private boolean isValidPayment(SslCommerzValidation v, Order order) {
        if (v == null) {
            return false;
        }
        String status = v.getStatus();
        if (!"VALID".equals(status) && !"VALIDATED".equals(status)) {
            return false;
        }
        if (!"BDT".equalsIgnoreCase(v.getCurrency())) {
            return false;
        }
        if (!order.getId().equals(v.getTranId())) {
            return false;
        }
        try {
            // compare as numbers, so "500.00" and "500" are equal
            return new BigDecimal(v.getAmount()).compareTo(BigDecimal.valueOf(order.getTotal())) == 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- paths

    private String successPath(String orderId) {
        return "/app/pro/result/success?order=" + orderId;
    }

    private String failedPath(String orderId) {
        return orderId == null ? "/app/pro/result/failed" : "/app/pro/result/failed?order=" + orderId;
    }

    private String cancelledPath(String orderId) {
        return orderId == null ? "/app/pro/result/cancelled" : "/app/pro/result/cancelled?order=" + orderId;
    }

    // ---------------------------------------------------------------- helpers

    private String methodName(String cardType, String valId) {
        String base = cardType == null || cardType.isBlank() ? "SSLCommerz" : cardType;
        if (valId != null && valId.length() >= 4) {
            return base + " ···· " + valId.substring(valId.length() - 4);
        }
        return base;
    }

    private Plan findActivePlan(String planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        if (!plan.isActive()) {
            throw new BadRequestException("This plan is no longer available");
        }
        return plan;
    }

    /**
     * Accepts a code only while it exists, is active, is not expired and still has uses.
     * Anything else is a BadRequest, so a typo is seen instead of silently paying full price.
     */
    private Coupon requireValidCoupon(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired coupon"));
        if (!coupon.isActive()) {
            throw new BadRequestException("Invalid or expired coupon");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDate.now())) {
            throw new BadRequestException("Invalid or expired coupon");
        }
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Invalid or expired coupon");
        }
        return coupon;
    }

    /**
     * PERCENT: value% of the price (a value above 100 is treated as 100).
     * FLAT: value directly, but never more than the price minus 1.
     * In both cases the total can never drop below 1 BDT.
     */
    private PriceBreakdown breakdown(int price, Coupon coupon) {
        PriceBreakdown b = new PriceBreakdown();
        b.discount = 0;
        b.total = price;
        if (coupon == null) {
            return b;
        }
        if (coupon.getType() == CouponType.PERCENT) {
            int percent = Math.min(Math.max(coupon.getValue(), 0), 100);
            b.discount = (int) Math.round(price * percent / 100.0);
        } else {
            b.discount = Math.min(Math.max(coupon.getValue(), 0), price - 1);
        }
        b.total = Math.max(1, price - b.discount);
        return b;
    }

    /**
     * A fresh transaction id for SSLCommerz: "GR-" + the current time in milliseconds + a small
     * random tail. Only letters, digits and a hyphen are used (the gateway's requirement),
     * and it is retried if it somehow already exists.
     */
    private String uniqueOrderId() {
        for (int attempt = 0; attempt < 25; attempt++) {
            String candidate = "GR-" + System.currentTimeMillis() + "-" + (100 + ThreadLocalRandom.current().nextInt(900));
            if (!orderRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique order id");
    }

    private static final class PriceBreakdown {
        private int discount;
        private int total;
    }
}
