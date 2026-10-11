package com.gamerift.feature.match.service;

import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.match.dto.MatchRequest;
import com.gamerift.feature.match.dto.MatchResponse;
import com.gamerift.feature.match.model.MatchCard;
import com.gamerift.feature.match.model.MatchStatus;
import com.gamerift.feature.match.repository.MatchCardRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for match cards (layer: service).
 * Called by: MatchController (public read) and AdminMatchController (create / update / delete).
 * Calls: MatchCardRepository (MongoDB collection "matches").
 * Rules: the two teams must differ; the list is sorted LIVE -> HALF_TIME -> ENDED.
 */
@Service // @Service = Spring creates one object of this class and injects it where needed
public class MatchServiceImpl implements IMatchService {

    private final MatchCardRepository matchRepository;

    // constructor injection: Spring passes the repository in
    public MatchServiceImpl(MatchCardRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    public List<MatchResponse> getAll(String status) {
        // 1. read the optional status filter
        MatchStatus wanted = parseStatus(status);

        // 2. load the cards and keep the ones that match the filter
        List<MatchCard> cards = new ArrayList<>();
        for (MatchCard card : matchRepository.findAll()) {
            if (wanted == null || card.getStatus() == wanted) {
                cards.add(card);
            }
        }

        // 3. sort: enum order is LIVE, HALF_TIME, ENDED, so ordinal() gives the order we want
        cards.sort((a, b) -> Integer.compare(a.getStatus().ordinal(), b.getStatus().ordinal()));

        // 4. convert to DTOs
        List<MatchResponse> result = new ArrayList<>();
        for (MatchCard card : cards) {
            result.add(MatchMapper.toResponse(card));
        }
        return result;
    }

    @Override
    public MatchResponse create(MatchRequest request) {
        // 1. business rule
        checkTeamsDiffer(request);
        // 2. build and save
        MatchCard card = new MatchCard();
        MatchMapper.copyInto(card, request);
        return MatchMapper.toResponse(matchRepository.save(card));
    }

    @Override
    public MatchResponse update(String id, MatchRequest request) {
        // 1. the card must exist
        MatchCard card = matchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Match not found"));
        // 2. business rule
        checkTeamsDiffer(request);
        // 3. overwrite the fields and save
        MatchMapper.copyInto(card, request);
        return MatchMapper.toResponse(matchRepository.save(card));
    }

    @Override
    public void delete(String id) {
        // 1. the card must exist
        if (!matchRepository.existsById(id)) {
            throw new NotFoundException("Match not found");
        }
        // 2. delete
        matchRepository.deleteById(id);
    }

    private void checkTeamsDiffer(MatchRequest request) {
        if (request.getTeamA().trim().equalsIgnoreCase(request.getTeamB().trim())) {
            throw new BadRequestException("Team A and Team B must be different");
        }
    }

    private MatchStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MatchStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status must be LIVE, HALF_TIME or ENDED");
        }
    }
}
