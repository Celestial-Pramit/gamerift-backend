package com.gamerift.feature.payment.service;

import java.util.ArrayList;

import com.gamerift.feature.payment.dto.CouponResponse;
import com.gamerift.feature.payment.dto.OrderResponse;
import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.model.Coupon;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.Plan;

/** Copies plans, coupons and orders into their response DTOs. Pure mapping, no logic. */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    static PlanResponse toPlanResponse(Plan plan) {
        PlanResponse r = new PlanResponse();
        r.setId(plan.getId());
        r.setName(plan.getName());
        r.setDays(plan.getDays());
        r.setPriceBdt(plan.getPriceBdt());
        r.setTagline(plan.getTagline() == null ? "" : plan.getTagline());
        r.setPerks(plan.getPerks() == null ? new ArrayList<>() : new ArrayList<>(plan.getPerks()));
        r.setBestValue(plan.isBestValue());
        r.setActive(plan.isActive());
        return r;
    }

    static CouponResponse toCouponResponse(Coupon coupon) {
        CouponResponse r = new CouponResponse();
        r.setId(coupon.getId());
        r.setCode(coupon.getCode());
        r.setType(coupon.getType());
        r.setValue(coupon.getValue());
        r.setMaxUses(coupon.getMaxUses());
        r.setUsedCount(coupon.getUsedCount());
        r.setExpiresAt(coupon.getExpiresAt());
        r.setActive(coupon.isActive());
        return r;
    }

    static OrderResponse toOrderResponse(Order order) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setPlanId(order.getPlanId());
        r.setPlanName(order.getPlanName());
        r.setDays(order.getDays());
        r.setPriceBdt(order.getPriceBdt());
        r.setCode(order.getCode());
        r.setDiscount(order.getDiscount());
        r.setTotal(order.getTotal());
        r.setMethod(order.getMethod());
        r.setStatus(order.getStatus());
        r.setCreatedAt(order.getCreatedAt());
        return r;
    }
}