package com.gamerift.feature.payment.service;

import java.util.List;

import com.gamerift.feature.payment.dto.CouponResponse;
import com.gamerift.feature.payment.dto.PlanRequest;
import com.gamerift.feature.payment.dto.PlanResponse;

/** Admin service for the "Go Pro" plans (admin panel "Plans" screen). */
public interface IAdminPlanService {

    List<PlanResponse> getAll();

    PlanResponse create(PlanRequest request);

    PlanResponse update(String id, PlanRequest request);

    void delete(String id);
}