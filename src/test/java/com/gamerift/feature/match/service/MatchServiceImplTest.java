package com.gamerift.feature.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.match.dto.MatchRequest;
import com.gamerift.feature.match.dto.MatchResponse;
import com.gamerift.feature.match.model.MatchCard;
import com.gamerift.feature.match.model.MatchStatus;
import com.gamerift.feature.match.repository.MatchCardRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the match rules (sorting, filter, validation, CRUD). The repository is a fake. */
@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchCardRepository matchRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    private MatchCard card(String id, MatchStatus status) {
        MatchCard c = new MatchCard();
        c.setId(id);
        c.setTeamA("Alpha");
        c.setTeamB("Beta");
        c.setStatus(status);
        return c;
    }

    private MatchRequest request(String teamA, String teamB) {
        MatchRequest r = new MatchRequest();
        r.setTeamA(teamA);
        r.setTeamB(teamB);
        r.setScoreA(2);
        r.setScoreB(1);
        r.setStatus(MatchStatus.LIVE);
        r.setInfo("MAP 3");
        return r;
    }

    @Test
    void getAll_sortsLiveFirstThenHalfTimeThenEnded() {
        when(matchRepository.findAll()).thenReturn(
                List.of(card("e", MatchStatus.ENDED), card("h", MatchStatus.HALF_TIME), card("l", MatchStatus.LIVE)));

        List<MatchResponse> result = matchService.getAll(null);

        assertEquals("l", result.get(0).getId());
        assertEquals("h", result.get(1).getId());
        assertEquals("e", result.get(2).getId());
    }

    @Test
    void getAll_withStatus_keepsOnlyThatStatus() {
        when(matchRepository.findAll()).thenReturn(
                List.of(card("e", MatchStatus.ENDED), card("l", MatchStatus.LIVE)));

        List<MatchResponse> result = matchService.getAll("ended");

        assertEquals(1, result.size());
        assertEquals("e", result.get(0).getId());
    }

    @Test
    void getAll_unknownStatus_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> matchService.getAll("paused"));
    }

    @Test
    void create_savesTheCard() {
        when(matchRepository.save(any(MatchCard.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchResponse response = matchService.create(request("  NightRaid ", "Nova"));

        assertEquals("NightRaid", response.getTeamA());
        assertEquals("LIVE", response.getStatus());
        verify(matchRepository).save(any(MatchCard.class));
    }

    @Test
    void create_sameTeamTwice_throwsBadRequestAndSavesNothing() {
        assertThrows(BadRequestException.class, () -> matchService.create(request("Alpha", " alpha ")));
        verify(matchRepository, never()).save(any(MatchCard.class));
    }

    @Test
    void update_changesTheFields() {
        when(matchRepository.findById("m1")).thenReturn(Optional.of(card("m1", MatchStatus.LIVE)));
        when(matchRepository.save(any(MatchCard.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchResponse response = matchService.update("m1", request("Red", "Blue"));

        assertEquals("Red", response.getTeamA());
        assertEquals(2, response.getScoreA());
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(matchRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> matchService.update("zzz", request("Red", "Blue")));
    }

    @Test
    void delete_existingCard_deletesIt() {
        when(matchRepository.existsById("m1")).thenReturn(true);

        matchService.delete("m1");

        verify(matchRepository).deleteById("m1");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(matchRepository.existsById("zzz")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> matchService.delete("zzz"));
        verify(matchRepository, never()).deleteById(any());
    }
}
