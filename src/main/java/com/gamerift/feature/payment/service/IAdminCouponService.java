package com.gamerift.feature.payment.service;

import java.util.List;

import com.gamerift.feature.payment.dto.CouponRequest;
import com.gamerift.feature.payment.dto.CouponResponse;

/** Admin service for discount codes (admin panel "Coupons" screen). */
public interface IAdminCouponService {

    List<CouponResponse> getAll();

    CouponResponse create(CouponRequest request);

    CouponResponse update(String id, CouponRequest request);

    void delete(String id);
}