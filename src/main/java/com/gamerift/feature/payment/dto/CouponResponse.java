package com.gamerift.feature.payment.dto;

import java.time.LocalDate;

import com.gamerift.feature.payment.model.CouponType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A discount code as the admin table renders it. */
@Getter
@Setter
@NoArgsConstructor
public class CouponResponse {

    private String id;
    private String code;
    private CouponType type;
    private int value;
    private int maxUses;
    private int usedCount;
    private LocalDate expiresAt;
    private boolean active;
}