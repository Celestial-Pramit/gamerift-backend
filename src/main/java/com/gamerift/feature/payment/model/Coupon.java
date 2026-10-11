package com.gamerift.feature.payment.model;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A discount code (collection "coupons").
 * The server checks: exists, active, not expired, usedCount < maxUses (in a later phase).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "coupons")
public class Coupon {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private CouponType type = CouponType.PERCENT;
    private int value;
    private int maxUses;
    private int usedCount;
    private LocalDate expiresAt;
    private boolean active = true;
}
