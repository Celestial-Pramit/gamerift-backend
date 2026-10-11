package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

import org.junit.jupiter.api.Test;

/** The formula is a pure function of the stream and "now", so it is tested with exact numbers. */
class TrendScoreCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    private LiveStream live(int viewers, int likes, Instant startedAt) {
        LiveStream s = new LiveStream();
        s.setPhase(StreamPhase.LIVE);
        s.setBaseViewers(viewers);
        s.setLikesCount(likes);
        s.setStartedAt(startedAt);
        return s;
    }

    @Test
    void streamThatIsNotLive_scoresZero() {
        LiveStream s = live(10_000, 1000, NOW);
        s.setPhase(StreamPhase.ENDED);

        assertEquals(0.0, TrendScoreCalculator.score(s, NOW));
    }

    @Test
    void justStartedStream_getsTheFullFreshnessBonus() {
        // 10 (viewers) + 2 (likes) + 10 (freshness)
        assertEquals(22.0, TrendScoreCalculator.score(live(10_000, 1000, NOW), NOW));
    }

    @Test
    void freshnessFallsOverTime() {
        // started 12 hours ago: freshness 5
        assertEquals(17.0, TrendScoreCalculator.score(live(10_000, 1000, NOW.minusSeconds(12 * 3600)), NOW));
        // started 24 hours ago or more: freshness 0
        assertEquals(12.0, TrendScoreCalculator.score(live(10_000, 1000, NOW.minusSeconds(24 * 3600)), NOW));
    }

    @Test
    void scoreIsRoundedToOneDecimal() {
        // 1234 viewers = 1.234 -> 1.2 (started 30 hours ago, no likes)
        assertEquals(1.2, TrendScoreCalculator.score(live(1234, 0, NOW.minusSeconds(30 * 3600)), NOW));
    }
}
