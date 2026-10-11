package com.gamerift.feature.match.service;

import java.util.List;

import com.gamerift.feature.match.dto.MatchRequest;
import com.gamerift.feature.match.dto.MatchResponse;

/** What the match feature can do: read the cards (everyone) and manage them (admin). */
public interface IMatchService {

    /** All match cards, LIVE first, then HALF_TIME, then ENDED. status is optional (case does not matter). */
    List<MatchResponse> getAll(String status);

    /** Creates a card. The two teams must be different. */
    MatchResponse create(MatchRequest request);

    /** Updates a card. Throws 404 if the id does not exist. */
    MatchResponse update(String id, MatchRequest request);

    /** Deletes a card. Throws 404 if the id does not exist. */
    void delete(String id);
}
