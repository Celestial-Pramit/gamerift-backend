package com.gamerift.feature.match.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A match card as the frontend sees it (layer: DTO).
 * status is the enum name as text: LIVE, HALF_TIME or ENDED.
 * Scores and odds are display only (they are not real betting data).
 */
@Getter
@Setter
@NoArgsConstructor
public class MatchResponse {

    private String id;
    private String teamA;
    private String teamB;
    private int scoreA;
    private int scoreB;
    private String oddsA;
    private String oddsDraw;
    private String oddsB;
    private String status;
    private String info;
}
