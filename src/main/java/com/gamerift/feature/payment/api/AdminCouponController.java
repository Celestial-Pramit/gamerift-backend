package com.gamerift.feature.payment.api;

import java.util.List;

import com.gamerift.feature.payment.dto.CouponRequest;
import com.gamerift.feature.payment.dto.CouponResponse;
import com.gamerift.feature.payment.service.IAdminCouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only management of discount codes (admin panel "Coupons" screen). */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Coupons", description = "Create / edit / delete discount codes (ADMIN only)")
public class AdminCouponController {

    private final IAdminCouponService adminCouponService;

    public AdminCouponController(IAdminCouponService adminCouponService) {
        this.adminCouponService = adminCouponService;
    }

    @GetMapping
    @Operation(summary = "All coupons")
    public List<CouponResponse> getAll() {
        return adminCouponService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon")
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        return adminCouponService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon")
    public CouponResponse update(@PathVariable String id, @Valid @RequestBody CouponRequest request) {
        return adminCouponService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a coupon")
    public void delete(@PathVariable String id) {
        adminCouponService.delete(id);
    }
}