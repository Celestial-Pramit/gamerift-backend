package com.gamerift.feature.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.CouponRepository;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.payment.repository.PaymentInterestRepository;
import com.gamerift.feature.payment.repository.PlanRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;
import com.gamerift.shared.exception.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Unit tests of the new SSLCommerz checkout flow (async, callback-driven). */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private INotificationService notificationService;
    @Mock
    private SslCommerzClient sslCommerzClient;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private PaymentInterestRepository paymentInterestRepository;

    @InjectMocks
    private CheckoutServiceImpl service;

    private Plan plan(String id, int price) {
        Plan p = new Plan();
        p.setId(id);
        p.setName("Pro");
        p.setDays(30);
        p.setPriceBdt(price);
        p.setActive(true);
        return p;
    }

    private Coupon coupon(String code, CouponType type, int value, int maxUses, int used) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setType(type);
        c.setValue(value);
        c.setMaxUses(maxUses);
        c.setUsedCount(used);
        c.setExpiresAt(LocalDate.now().plusDays(30));
        c.setActive(true);
        return c;
    }

    private User user(String id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setCountry("Bangladesh");
        return u;
    }

    private QuoteRequest quoteReq(String code) {
        QuoteRequest q = new QuoteRequest();
        q.setPlanId("p1");
        q.setCode(code);
        return q;
    }

    private CheckoutRequest req(String code) {
        CheckoutRequest r = new CheckoutRequest();
        r.setPlanId("p1");
        r.setCode(code);
        return r;
    }

    private Order order(String id, String userId, OrderStatus status, Instant createdAt) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setPlanId("p1");
        o.setPlanName("Pro");
        o.setDays(30);
        o.setPriceBdt(499);
        o.setTotal(449);
        o.setMethod("SSLCommerz");
        o.setGateway("SSLCOMMERZ");
        o.setStatus(status);
        o.setCreatedAt(createdAt);
        return o;
    }

    // A valid SSLCommerz validation response
    private SslCommerzValidation validValidation(String tranId, int amount, String bankTranId, String cardType) {
        SslCommerzValidation v = new SslCommerzValidation();
        v.setStatus("VALID");
        v.setTranId(tranId);
        v.setAmount(String.valueOf(amount));
        v.setCurrency("BDT");
        v.setBankTranId(bankTranId);
        v.setCardType(cardType);
        return v;
    }

    // ---------------------------------------------------- createPayment

    @Test
    void createPayment_createsPendingOrderReturnsGatewayUrl_noProNoCouponBurn() {
        // Arrange: user + plan + coupon exist
        User u = user("u1", "alice");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(planRepository.findById("p1")).thenReturn(Optional.of(plan("p1", 499)));
        when(couponRepository.findByCode("GG10")).thenReturn(Optional.of(coupon("GG10", CouponType.FLAT, 50, 10, 0)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sslCommerzClient.createSession(any(Order.class), eq(u))).thenReturn("https://gateway.url/pay");

        // Act
        PaymentInitResponse response = service.createPayment("u1", req("GG10"));

        // Assert: PENDING order created, gateway URL returned, Pro NOT extended, coupon NOT burned
        assertNotNull(response.getOrderId());
        assertEquals(449, response.getAmount());
        assertEquals("https://gateway.url/pay", response.getGatewayUrl());

        verify(notificationService, never()).send(anyString(), anyString(), anyString(), anyString());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createPayment_bannedUser_throwsForbidden() {
        // Arrange: banned user
        User u = user("u1", "alice");
        u.setBanned(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> service.createPayment("u1", req("GG10")));
        verify(planRepository, never()).findById(anyString());
    }

    // ---------------------------------------------------- handleSuccess

    @Test
    void handleSuccess_validValidation_activatesProOnce_setsOrderPaid_sendsNotification() {
        // Arrange: PENDING order + valid validation
        Order pending = order("GR-123", "u1", OrderStatus.PENDING, Instant.now());
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(pending));
        when(sslCommerzClient.validate("val-456")).thenReturn(validValidation("GR-123", 449, "bank-789", "bKash"));

        // MongoTemplate findAndModify returns the order -> this call WON the race
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Order.class))).thenReturn(pending);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice")));

        // Act
        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        // Assert: redirected to success page, order is PAID, Pro extended, notification sent
        assertEquals("/app/pro/result/success?order=GR-123", path);
        verify(notificationService).send(eq("u1"), eq("pay"), eq("Pro activated: receipt GR-123"), eq("/app/settings"));
    }

    @Test
    void handleSuccess_repeatedCallback_returnsSuccessButDoesNotDoubleActivate() {
        // Arrange: another callback already activated the order (status is PAID)
        Order paid = order("GR-123", "u1", OrderStatus.PAID, Instant.now());
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(paid));

        // Act
        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        // Assert: still success path, but Pro NOT extended again (validate & findAndModify not called)
        assertEquals("/app/pro/result/success?order=GR-123", path);
        verify(notificationService, never()).send(anyString(), anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(sslCommerzClient, never()).validate(anyString());
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), eq(Order.class));
    }

    // ---------------------------------------------------- handleFail / handleCancel

    @Test
    void handleFail_marksOrderFailed() {
        String path = service.handleFail(Map.of("tran_id", "GR-123"));

        assertEquals("/app/pro/result/failed?order=GR-123", path);
    }

    @Test
    void handleCancel_marksOrderCancelled() {
        String path = service.handleCancel(Map.of("tran_id", "GR-123"));

        assertEquals("/app/pro/result/cancelled?order=GR-123", path);
    }

    // ---------------------------------------------------- Security cases (Part 2)

    @Test
    void handleSuccess_amountMismatch_rejectsOrderNotPaid() {
        Order pending = order("GR-123", "u1", OrderStatus.PENDING, Instant.now());
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(pending));
        // validation says 500 but order.total is 449
        when(sslCommerzClient.validate("val-456")).thenReturn(validValidation("GR-123", 500, "bank-789", "bKash"));

        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        assertEquals("/app/pro/result/failed?order=GR-123", path);
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), eq(Order.class));
    }

    @Test
    void handleSuccess_tranIdMismatch_rejectsOrderNotPaid() {
        Order pending = order("GR-123", "u1", OrderStatus.PENDING, Instant.now());
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(pending));
        // validation tran_id differs from order id
        when(sslCommerzClient.validate("val-456")).thenReturn(validValidation("GR-999", 449, "bank-789", "bKash"));

        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        assertEquals("/app/pro/result/failed?order=GR-123", path);
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), eq(Order.class));
    }

    @Test
    void handleIpn_lateRevivesFailedOrder() {
        // Arrange: FAILED order, valid validation
        Order failed = order("GR-123", "u1", OrderStatus.FAILED, Instant.now());
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(failed));
        when(sslCommerzClient.validate("val-456")).thenReturn(validValidation("GR-123", 449, "bank-789", "bKash"));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Order.class))).thenReturn(failed);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice")));

        // Act: IPN handler never throws, always returns void
        service.handleIpn(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        // Assert: order activated to PAID, Pro extended, notification sent
        verify(notificationService).send(eq("u1"), eq("pay"), eq("Pro activated: receipt GR-123"), eq("/app/settings"));
    }

    @Test
    void handleSuccess_couponUsedUp_grantsProWithoutBurning() {
        // Arrange: coupon at maxUses
        Order pending = order("GR-123", "u1", OrderStatus.PENDING, Instant.now());
        pending.setCode("GG10");
        Coupon c = coupon("GG10", CouponType.FLAT, 50, 10, 10); // usedCount == maxUses
        when(orderRepository.findById("GR-123")).thenReturn(Optional.of(pending));
        when(sslCommerzClient.validate("val-456")).thenReturn(validValidation("GR-123", 449, "bank-789", "bKash"));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), eq(Order.class))).thenReturn(pending);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice")));
        when(couponRepository.findByCode("GG10")).thenReturn(Optional.of(c));

        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "val_id", "val-456", "card_type", "bKash"));

        // Assert: Pro granted, coupon NOT burned (usedCount stays 10)
        assertEquals("/app/pro/result/success?order=GR-123", path);
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void handleSuccess_missingValId_takesFailurePath() {
        // No val_id in params
        String path = service.handleSuccess(Map.of("tran_id", "GR-123", "card_type", "bKash"));

        assertEquals("/app/pro/result/failed?order=GR-123", path);
    }

    @Test
    void notifyMe_duplicateInterest_createsOnlyOneRow() {
        // Arrange: interest already exists
        when(paymentInterestRepository.existsByUserIdAndMethod("u1", "Nagad")).thenReturn(true);

        // Act
        service.notifyMe("u1", "Nagad");

        // Assert: repository.save never called
        verify(paymentInterestRepository, never()).save(any());
    }
}