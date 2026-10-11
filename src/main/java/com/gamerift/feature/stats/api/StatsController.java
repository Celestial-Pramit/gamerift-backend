package com.gamerift.feature.stats.api;

import java.util.List;

import com.gamerift.feature.stats.dto.CountryStatResponse;
import com.gamerift.feature.stats.dto.PublicStatsResponse;
import com.gamerift.feature.stats.service.IStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statistics endpoints.
 *  - /public needs NO login (SecurityConfig lists it as public): the landing page numbers.
 *  - /users-by-country needs a login (dashboard chart).
 * The admin overview statistics arrive in Phase 9.
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats", description = "Users by country and public landing page numbers")
public class StatsController {

    private final IStatsService statsService;

    public StatsController(IStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/users-by-country")
    @Operation(summary = "Users per country (MongoDB aggregation): top 5 plus 'Rest of world'")
    public List<CountryStatResponse> getUsersByCountry() {
        return statsService.getUsersByCountry();
    }

    @GetMapping("/public")
    @Operation(summary = "Real numbers for the landing page (public, no login)")
    public PublicStatsResponse getPublicStats() {
        return statsService.getPublicStats();
    }
}
