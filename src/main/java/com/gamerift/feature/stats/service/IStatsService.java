package com.gamerift.feature.stats.service;

import java.util.List;

import com.gamerift.feature.stats.dto.AdminStatsResponse;
import com.gamerift.feature.stats.dto.CountryStatResponse;
import com.gamerift.feature.stats.dto.PublicStatsResponse;

/** Numbers for charts and for the landing page. */
public interface IStatsService {

    /** The 5 biggest countries and one "Rest of world" row for all the others. */
    List<CountryStatResponse> getUsersByCountry();

    PublicStatsResponse getPublicStats();

    /** Numbers for the admin overview (users, live streams, comments, revenue, charts). */
    AdminStatsResponse getAdminStats();
}
