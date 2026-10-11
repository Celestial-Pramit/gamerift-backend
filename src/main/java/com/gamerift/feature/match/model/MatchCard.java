package com.gamerift.feature.match.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** A score card shown on the dashboard (collection "matches"). Scores and odds are display only. */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "matches")
public class MatchCard {

    @Id
    private String id;

    private String teamA;
    private String teamB;
    private int scoreA;
    private int scoreB;
    private String oddsA;
    private String oddsDraw;
    private String oddsB;
    private MatchStatus status = MatchStatus.LIVE;
    // For example "MAP 3 · 12:04"
    private String info;
}
