package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

import org.junit.jupiter.api.Test;

/** The mapper gets a fixed "now", so the minute calculations can be tested exactly. */
class StreamMapperTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    private LiveStream stream(StreamPhase phase) {
        LiveStream s = new LiveStream();
        s.setId("s1");
        s.setTitle("Title");
        s.setCategorySlug("fps");
        s.setPhase(phase);
        s.setBaseViewers(5000);
        return s;
    }

    @Test
    void liveStream_hasViewersAndMinutesSinceStart() {
        LiveStream s = stream(StreamPhase.LIVE);
        s.setStartedAt(NOW.minusSeconds(90 * 60));

        StreamResponse r = StreamMapper.toResponse(s, true, NOW);

        assertEquals("live", r.getStatus());
        assertEquals(5000, r.getViewers());
        assertEquals(90, r.getMinutes());
        assertEquals("FPS", r.getLabel());
        assertTrue(r.isPro());
    }

    @Test
    void soonStream_hasNoViewersAndMinutesUntilStart() {
        LiveStream s = stream(StreamPhase.SOON);
        s.setScheduledAt(NOW.plusSeconds(30 * 60 + 10));

        StreamResponse r = StreamMapper.toResponse(s, false, NOW);

        assertEquals("soon", r.getStatus());
        assertEquals(0, r.getViewers());
        assertEquals(30, r.getMinutes());
    }

    @Test
    void endedStream_hasZeroViewersAndMinutes() {
        StreamResponse r = StreamMapper.toResponse(stream(StreamPhase.ENDED), false, NOW);

        assertEquals("ended", r.getStatus());
        assertEquals(0, r.getViewers());
        assertEquals(0, r.getMinutes());
    }
}
