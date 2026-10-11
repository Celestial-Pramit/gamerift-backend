package com.gamerift.feature.payment.repository;

import com.gamerift.feature.payment.model.PaymentInterest;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Database access for the "Notify me" interests (collection "paymentInterest"). */
public interface PaymentInterestRepository extends MongoRepository<PaymentInterest, String> {

    /** Used so a user cannot register the same interest twice (no duplicates). */
    boolean existsByUserIdAndMethod(String userId, String method);
}
