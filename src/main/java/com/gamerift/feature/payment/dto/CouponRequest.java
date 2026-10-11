package com.gamerift.feature.payment.dto;

import java.time.LocalDate;

import com.gamerift.feature.payment.model.CouponType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin create / update coupon requests. */
@Getter
@Setter
@NoArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code can have at most 20 characters")
    private String code;

    @NotNull(message = "Type is required")
    private CouponType type;

    @NotNull(message = "Value is required")
    @Min(value = 1, message = "Value must be at least 1")
    private Integer value;

    @NotNull(message = "Max uses is required")
    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private LocalDate expiresAt;

    private boolean active = true;
}