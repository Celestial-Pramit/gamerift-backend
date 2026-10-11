package com.gamerift.feature.payment.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A Pro plan as the Go Pro page and the admin table render it. */
@Getter
@Setter
@NoArgsConstructor
public class PlanResponse {

    private String id;
    private String name;
    private int days;
    private int priceBdt;
    private String tagline;
    private List<String> perks = new ArrayList<>();
    private boolean bestValue;
    private boolean active;
}