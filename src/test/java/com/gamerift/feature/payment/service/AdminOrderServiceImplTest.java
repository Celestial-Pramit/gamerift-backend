package com.gamerift.feature.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.gamerift.feature.payment.dto.AdminOrderResponse;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the read-only admin order list (username lookup). */
@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AdminOrderServiceImpl service;

    private Order order(String userId) {
        Order o = new Order();
        o.setId("GR-1");
        o.setUserId(userId);
        o.setPlanName("Squad");
        o.setTotal(249);
        o.setStatus(OrderStatus.PAID);
        o.setCreatedAt(Instant.now());
        return o;
    }

    @Test
    void getAll_showsTheUsernameNotTheId() {
        User u = new User();
        u.setId("u1");
        u.setUsername("gamer");
        when(orderRepository.findAll()).thenReturn(List.of(order("u1")));
        when(userRepository.findAllById(any())).thenReturn(List.of(u));

        List<AdminOrderResponse> result = service.getAll();

        assertEquals("gamer", result.get(0).getUser());
        assertEquals(249, result.get(0).getAmount());
    }

    @Test
    void getAll_deletedBuyer_showsAPlaceholder() {
        when(orderRepository.findAll()).thenReturn(List.of(order("gone")));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        assertEquals("(deleted user)", service.getAll().get(0).getUser());
    }

    @Test
    void getAll_noOrders_skipsTheUserLookup() {
        when(orderRepository.findAll()).thenReturn(List.of());

        assertEquals(0, service.getAll().size());
        verifyNoInteractions(userRepository);
    }
}