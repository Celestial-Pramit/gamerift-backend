package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.gamerift.feature.stream.job.TrendScoreJob;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendScoreJobTest {

    @Mock
    private LiveStreamRepository streamRepository;
    @Captor
    private ArgumentCaptor<List<LiveStream>> savedStreams;

    @InjectMocks
    private TrendScoreJob job;

    private LiveStream stream(StreamPhase phase, int viewers, double oldScore) {
        LiveStream s = new LiveStream();
        s.setPhase(phase);
        s.setBaseViewers(viewers);
        s.setTrendScore(oldScore);
        s.setStartedAt(Instant.now());
        return s;
    }

    @Test
    void recalculate_savesOnlyTheStreamsWhoseScoreChanged() {
        LiveStream changed = stream(StreamPhase.LIVE, 10_000, 0);
        LiveStream unchanged = stream(StreamPhase.ENDED, 5000, 0);
        when(streamRepository.findAll()).thenReturn(List.of(changed, unchanged));

        job.recalculate();

        verify(streamRepository).saveAll(savedStreams.capture());
        assertEquals(1, savedStreams.getValue().size());
        assertEquals(changed, savedStreams.getValue().get(0));
    }

    @Test
    void recalculate_whenNothingChanged_savesNothing() {
        when(streamRepository.findAll()).thenReturn(List.of(stream(StreamPhase.ENDED, 5000, 0)));

        job.recalculate();

        verify(streamRepository, never()).saveAll(any());
    }
}
