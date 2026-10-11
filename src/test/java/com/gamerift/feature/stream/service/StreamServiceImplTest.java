package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.stream.repository.StreamSearchRepository;
import com.gamerift.shared.dto.PageResponse;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit tests of the read rules of streams. The repositories are fakes, so no database is needed. */
@ExtendWith(MockitoExtension.class)
class StreamServiceImplTest {

    @Mock
    private LiveStreamRepository liveStreamRepository;
    @Mock
    private StreamSearchRepository streamSearchRepository;
    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private StreamServiceImpl streamService;

    private LiveStream stream(String id, String channelId, boolean published) {
        LiveStream s = new LiveStream();
        s.setId(id);
        s.setTitle("Stream " + id);
        s.setChannelId(channelId);
        s.setCategorySlug("fps");
        s.setPhase(StreamPhase.LIVE);
        s.setPublished(published);
        s.setBaseViewers(100);
        return s;
    }

    private Channel proChannel(String id) {
        Channel c = new Channel();
        c.setId(id);
        c.setPro(true);
        return c;
    }

    @Test
    void search_returnsTheMappedPageAndMarksProChannels() {
        when(streamSearchRepository.search(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(stream("s1", "c1", true)), PageRequest.of(0, 12), 1));
        when(channelRepository.findAllById(any())).thenReturn(List.of(proChannel("c1")));

        PageResponse<StreamResponse> response = streamService.search("rozu", "fps", null, 0, "live", "viewers", 0, 12);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertTrue(response.getContent().get(0).isPro());
    }

    @Test
    void search_limitsThePageSizeToFifty() {
        when(streamSearchRepository.search(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<LiveStream>(), PageRequest.of(0, 50), 0));

        streamService.search(null, null, null, 0, null, "viewers", -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(streamSearchRepository).search(any(), any(), any(), anyInt(), any(), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals(0, pageable.getValue().getPageNumber());
    }

    @Test
    void search_statusLive_isPassedAsThePhaseFilter() {
        when(streamSearchRepository.search(any(), any(), any(), anyInt(), eq(StreamPhase.LIVE), any()))
                .thenReturn(new PageImpl<>(new ArrayList<LiveStream>(), PageRequest.of(0, 12), 0));

        streamService.search(null, null, null, 0, "LIVE", "trending", 0, 12);

        verify(streamSearchRepository).search(any(), any(), any(), anyInt(), eq(StreamPhase.LIVE), any());
    }

    @Test
    void search_unknownSort_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> streamService.search(null, null, null, 0, null, "random", 0, 12));
    }

    @Test
    void search_unknownStatus_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> streamService.search(null, null, null, 0, "paused", "viewers", 0, 12));
    }

    @Test
    void getById_draft_looksLikeItDoesNotExist() {
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(stream("s1", "c1", false)));

        assertThrows(NotFoundException.class, () -> streamService.getById("s1"));
    }

    @Test
    void getById_publishedStream_isReturned() {
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(stream("s1", "c1", true)));
        when(channelRepository.findById("c1")).thenReturn(Optional.of(proChannel("c1")));

        StreamResponse response = streamService.getById("s1");

        assertEquals("s1", response.getId());
        assertTrue(response.isPro());
    }

    @Test
    void getFeatured_whenNoneExists_throwsNotFound() {
        when(liveStreamRepository.findFirstByPublishedTrueAndFeaturedTrueOrderByBaseViewersDesc()).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> streamService.getFeatured());
    }

    @Test
    void getRelated_leavesOutTheStreamItselfAndKeepsAtMostSix() {
        List<LiveStream> candidates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            candidates.add(stream("s" + i, "c1", true));
        }
        when(liveStreamRepository.findById("s0")).thenReturn(Optional.of(candidates.get(0)));
        when(liveStreamRepository.findTop7ByCategorySlugAndPublishedTrueOrderByBaseViewersDesc("fps")).thenReturn(candidates);

        List<StreamResponse> related = streamService.getRelated("s0");

        assertEquals(6, related.size());
        for (StreamResponse r : related) {
            assertFalse(r.getId().equals("s0"));
        }
    }

    @Test
    void getTrending_returnsTheMappedStreams() {
        when(liveStreamRepository.findTop8ByPublishedTrueAndPhaseOrderByTrendScoreDesc(StreamPhase.LIVE))
                .thenReturn(List.of(stream("s1", "c1", true), stream("s2", "c1", true)));

        assertEquals(2, streamService.getTrending().size());
    }
}
