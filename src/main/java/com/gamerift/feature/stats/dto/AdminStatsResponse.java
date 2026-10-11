package com.gamerift.feature.stats.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The numbers of the admin "Overview" screen. revenueByDay = the last 7 days, oldest first. */
@Getter
@Setter
@NoArgsConstructor
public class AdminStatsResponse {

    private long users;
    private long liveStreams;
    private long comments;
    // all-time income (PAID orders only)
    private long revenue;
    private List<CountryStatResponse> usersByCountry = new ArrayList<>();
    private List<CategoryCountResponse> streamsByCategory = new ArrayList<>();
    private List<Long> revenueByDay = new ArrayList<>();
}