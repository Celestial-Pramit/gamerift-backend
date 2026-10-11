package com.gamerift.feature.payment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gamerift.feature.payment.dto.PlanRequest;
import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.PlanRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/** Admin CRUD for Pro plans (layer: service). Deletion is soft: the plan stops being shown. */
@Service
public class AdminPlanServiceImpl implements IAdminPlanService {

    private final PlanRepository planRepository;

    public AdminPlanServiceImpl(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<PlanResponse> getAll() {
        List<Plan> plans = new ArrayList<>(planRepository.findAll());
        plans.sort(Comparator.comparingInt(Plan::getPriceBdt));
        List<PlanResponse> result = new ArrayList<>();
        for (Plan plan : plans) {
            result.add(PaymentMapper.toPlanResponse(plan));
        }
        return result;
    }

    @Override
    public PlanResponse create(PlanRequest request) {
        Plan plan = new Plan();
        copy(request, plan);
        return PaymentMapper.toPlanResponse(planRepository.save(plan));
    }

    @Override
    public PlanResponse update(String id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        copy(request, plan);
        return PaymentMapper.toPlanResponse(planRepository.save(plan));
    }

    @Override
    public void delete(String id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        // soft delete: an old order still has planName/days/price copied, so it stays readable
        plan.setActive(false);
        planRepository.save(plan);
    }

    private void copy(PlanRequest request, Plan plan) {
        plan.setName(request.getName());
        plan.setDays(request.getDays());
        plan.setPriceBdt(request.getPriceBdt());
        plan.setTagline(request.getTagline());
        plan.setPerks(request.getPerks() == null ? new ArrayList<>() : request.getPerks());
        plan.setBestValue(request.isBestValue());
        plan.setActive(request.isActive());
    }
}