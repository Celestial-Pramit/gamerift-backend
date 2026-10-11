package com.gamerift.feature.payment.repository;

import com.gamerift.feature.payment.model.Plan;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlanRepository extends MongoRepository<Plan, String> {
}
