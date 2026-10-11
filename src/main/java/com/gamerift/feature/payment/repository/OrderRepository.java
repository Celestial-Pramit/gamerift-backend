package com.gamerift.feature.payment.repository;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.payment.model.Order;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findByIdAndUserId(String id, String userId);
}