package com.gamerift.feature.payment.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gamerift.feature.payment.dto.AdminOrderResponse;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.springframework.stereotype.Service;

/**
 * The admin "Orders" screen: every order, newest first.
 * The buyer's username comes from ONE extra query for all buyers (not one query per order).
 * If the account was deleted the table shows "(deleted user)".
 */
@Service
public class AdminOrderServiceImpl implements IAdminOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminOrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<AdminOrderResponse> getAll() {
        // 1. all orders, newest first
        List<Order> orders = new ArrayList<>(orderRepository.findAll());
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 2. one query for all buyers: userId -> username
        Set<String> userIds = new HashSet<>();
        for (Order order : orders) {
            userIds.add(order.getUserId());
        }
        Map<String, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                names.put(user.getId(), user.getUsername());
            }
        }

        // 3. convert to DTOs
        List<AdminOrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            AdminOrderResponse r = new AdminOrderResponse();
            r.setId(order.getId());
            r.setUser(names.getOrDefault(order.getUserId(), "(deleted user)"));
            r.setPlan(order.getPlanName());
            r.setAmount(order.getTotal());
            r.setStatus(order.getStatus());
            r.setCreatedAt(order.getCreatedAt());
            result.add(r);
        }
        return result;
    }
}
