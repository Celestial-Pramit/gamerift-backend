package com.gamerift.feature.match.dto;

import com.gamerift.feature.match.model.MatchStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of the admin create / update match requests.
 * Simple rules are checked here with annotations (@Valid in the controller);
 * the rule "two different teams" is checked in the service.
 */
@Getter
@Setter
@NoArgsConstructor
public class MatchRequest {

    @NotBlank(message = "Team A is required")
    @Size(max = 40, message = "Team A can have at most 40 characters")
    private String teamA;

    @NotBlank(message = "Team B is required")
    @Size(max = 40, message = "Team B can have at most 40 characters")
    private String teamB;

    @Min(value = 0, message = "Score cannot be negative")
    @Max(value = 999, message = "Score is too large")
    private int scoreA;

    @Min(value = 0, message = "Score cannot be negative")
    @Max(value = 999, message = "Score is too large")
    private int scoreB;

    @Size(max = 10, message = "Odds can have at most 10 characters")
    private String oddsA;

    @Size(max = 10, message = "Odds can have at most 10 characters")
    private String oddsDraw;

    @Size(max = 10, message = "Odds can have at most 10 characters")
    private String oddsB;

    @NotNull(message = "Status is required (LIVE, HALF_TIME or ENDED)")
    private MatchStatus status;

    @Size(max = 40, message = "Info can have at most 40 characters")
    private String info;
}
