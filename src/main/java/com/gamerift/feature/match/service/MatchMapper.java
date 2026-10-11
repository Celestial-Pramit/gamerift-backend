package com.gamerift.feature.match.service;

import com.gamerift.feature.match.dto.MatchRequest;
import com.gamerift.feature.match.dto.MatchResponse;
import com.gamerift.feature.match.model.MatchCard;

/** Copies data between MatchCard (database) and MatchRequest / MatchResponse (JSON). No business rules here. */
public final class MatchMapper {

    private MatchMapper() {
    }

    static MatchResponse toResponse(MatchCard card) {
        MatchResponse r = new MatchResponse();
        r.setId(card.getId());
        r.setTeamA(card.getTeamA());
        r.setTeamB(card.getTeamB());
        r.setScoreA(card.getScoreA());
        r.setScoreB(card.getScoreB());
        r.setOddsA(card.getOddsA());
        r.setOddsDraw(card.getOddsDraw());
        r.setOddsB(card.getOddsB());
        r.setStatus(card.getStatus().name());
        r.setInfo(card.getInfo());
        return r;
    }

    /** Fills a MatchCard from the request (used for both create and update). */
    static void copyInto(MatchCard card, MatchRequest request) {
        card.setTeamA(request.getTeamA().trim());
        card.setTeamB(request.getTeamB().trim());
        card.setScoreA(request.getScoreA());
        card.setScoreB(request.getScoreB());
        card.setOddsA(request.getOddsA());
        card.setOddsDraw(request.getOddsDraw());
        card.setOddsB(request.getOddsB());
        card.setStatus(request.getStatus());
        card.setInfo(request.getInfo());
    }
}
