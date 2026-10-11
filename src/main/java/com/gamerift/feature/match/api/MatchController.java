package com.gamerift.feature.match.api;

import java.util.List;

import com.gamerift.feature.match.dto.MatchResponse;
import com.gamerift.feature.match.service.IMatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for the match score cards on the dashboard.
 * Request flow: Browser -> MatchController -> IMatchService (MatchServiceImpl)
 *   -> MatchCardRepository -> MongoDB -> List of MatchResponse JSON.
 */
@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Match score cards (display only)")
public class MatchController {

    private final IMatchService matchService;

    public MatchController(IMatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    @Operation(summary = "All match cards, live first. Optional ?status=LIVE|HALF_TIME|ENDED")
    public List<MatchResponse> getAll(@RequestParam(required = false) String status) {
        return matchService.getAll(status);
    }
}
