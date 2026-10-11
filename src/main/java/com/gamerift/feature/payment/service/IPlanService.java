package com.gamerift.feature.payment.service;

import java.util.List;

import com.gamerift.feature.payment.dto.PlanResponse;

/** What the Go Pro page needs: the plans you can actually buy. */
public interface IPlanService {

    /** All active plans, cheapest first. */
    List<PlanResponse> getActivePlans();
}