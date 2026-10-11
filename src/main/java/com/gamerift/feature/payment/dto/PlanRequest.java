package com.gamerift.feature.payment.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin create / update plan requests. */
@Getter
@Setter
@NoArgsConstructor
public class PlanRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 60, message = "Name can have at most 60 characters")
    private String name;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer days;

    @NotNull(message = "Price is required")
    @Min(value = 1, message = "Price must be at least 1 BDT")
    private Integer priceBdt;

    @Size(max = 120, message = "Tagline can have at most 120 characters")
    private String tagline;

    private List<String> perks = new ArrayList<>();

    private boolean bestValue;
    private boolean active = true;
}