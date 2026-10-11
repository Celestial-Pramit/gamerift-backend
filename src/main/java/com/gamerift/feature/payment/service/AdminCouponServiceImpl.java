package com.gamerift.feature.payment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gamerift.feature.payment.dto.CouponRequest;
import com.gamerift.feature.payment.dto.CouponResponse;
import com.gamerift.feature.payment.model.Coupon;
import com.gamerift.feature.payment.model.CouponType;
import com.gamerift.feature.payment.repository.CouponRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Admin CRUD for discount codes (layer: service).
 *  - codes are unique (case-insensitive): a duplicate code is a 409 conflict,
 *  - a percent coupon must stay between 1 and 100, a flat coupon at least 1 BDT.
 */
@Service
public class AdminCouponServiceImpl implements IAdminCouponService {

    private final CouponRepository couponRepository;

    public AdminCouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public List<CouponResponse> getAll() {
        List<Coupon> coupons = new ArrayList<>(couponRepository.findAll());
        coupons.sort(Comparator.comparing(Coupon::getCode));
        List<CouponResponse> result = new ArrayList<>();
        for (Coupon coupon : coupons) {
            result.add(PaymentMapper.toCouponResponse(coupon));
        }
        return result;
    }

    @Override
    public CouponResponse create(CouponRequest request) {
        checkValue(request);
        checkFreeCode(request.getCode(), null);
        Coupon coupon = new Coupon();
        copy(request, coupon);
        return PaymentMapper.toCouponResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse update(String id, CouponRequest request) {
        checkValue(request);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found"));
        checkFreeCode(request.getCode(), id);
        copy(request, coupon);
        return PaymentMapper.toCouponResponse(couponRepository.save(coupon));
    }

    @Override
    public void delete(String id) {
        if (!couponRepository.existsById(id)) {
            throw new NotFoundException("Coupon not found");
        }
        couponRepository.deleteById(id);
    }

    /**
     * Sanity rule for a valid discount: PERCENT codes must stay between 1 and 100,
     * FLAT codes must be at least 1 BDT.
     */
    private void checkValue(CouponRequest request) {
        if (request.getType() == CouponType.PERCENT && (request.getValue() < 1 || request.getValue() > 100)) {
            throw new BadRequestException("A percent coupon must be between 1 and 100");
        }
        if (request.getType() == CouponType.FLAT && request.getValue() < 1) {
            throw new BadRequestException("A flat coupon must be at least 1 BDT");
        }
    }

    /** Codes are stored upper case: a second coupon with the same code is a conflict. */
    private void checkFreeCode(String code, String selfId) {
        couponRepository.findByCode(code.toUpperCase().trim())
                .ifPresent(existing -> {
                    if (selfId == null || !selfId.equals(existing.getId())) {
                        throw new ConflictException("Coupon code already exists: " + code);
                    }
                });
    }

    private void copy(CouponRequest request, Coupon coupon) {
        coupon.setCode(request.getCode().toUpperCase().trim());
        coupon.setType(request.getType());
        coupon.setValue(request.getValue());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setExpiresAt(request.getExpiresAt());
        coupon.setActive(request.isActive());
    }
}