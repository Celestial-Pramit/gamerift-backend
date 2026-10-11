package com.gamerift.feature.stats.api;

import com.gamerift.feature.stats.dto.AdminStatsResponse;
import com.gamerift.feature.stats.service.IStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only numbers for the admin overview screen. */
@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Stats", description = "Headline numbers and charts of the admin overview (ADMIN only)")
public class AdminStatsController {

    private final IStatsService statsService;

    public AdminStatsController(IStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    @Operation(summary = "Users, live streams, comments, revenue, country + category charts")
    public AdminStatsResponse getAdminStats() {
        return statsService.getAdminStats();
    }
}