package com.gamerift.feature.payment.repository;

import java.util.Optional;

import com.gamerift.feature.payment.model.Coupon;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CouponRepository extends MongoRepository<Coupon, String> {

    Optional<Coupon> findByCode(String code);
}
