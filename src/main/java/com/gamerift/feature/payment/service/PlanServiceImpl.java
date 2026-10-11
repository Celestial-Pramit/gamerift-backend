package com.gamerift.feature.payment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.PlanRepository;

import org.springframework.stereotype.Service;

/** Business rule: the store only shows active plans, cheapest first. */
@Service
public class PlanServiceImpl implements IPlanService {

    private final PlanRepository planRepository;

    public PlanServiceImpl(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<PlanResponse> getActivePlans() {
        List<Plan> plans = new ArrayList<>(planRepository.findAll());
        plans.sort(Comparator.comparingInt(Plan::getPriceBdt));
        List<PlanResponse> result = new ArrayList<>();
        for (Plan plan : plans) {
            if (plan.isActive()) {
                result.add(PaymentMapper.toPlanResponse(plan));
            }
        }
        return result;
    }
}