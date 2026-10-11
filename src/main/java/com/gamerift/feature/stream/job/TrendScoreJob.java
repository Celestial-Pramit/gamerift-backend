package com.gamerift.feature.stream.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.stream.service.TrendScoreCalculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A scheduled job: Spring runs recalculate() by itself, 10 seconds after start-up and then every 5 minutes.
 * It refreshes the trendScore of every stream, so the "Trending" tab always sorts by fresh numbers.
 * Only streams whose score really changed are written back (one saveAll).
 *
 * @Scheduled needs @EnableScheduling (see SchedulingConfig).
 */
@Component
public class TrendScoreJob {

    private static final Logger log = LoggerFactory.getLogger(TrendScoreJob.class);

    private final LiveStreamRepository streamRepository;

    public TrendScoreJob(LiveStreamRepository streamRepository) {
        this.streamRepository = streamRepository;
    }

    @Scheduled(initialDelay = 10_000, fixedRate = 300_000)
    public void recalculate() {
        Instant now = Instant.now();
        List<LiveStream> changed = new ArrayList<>();

        for (LiveStream stream : streamRepository.findAll()) {
            double score = TrendScoreCalculator.score(stream, now);
            if (Math.abs(score - stream.getTrendScore()) > 0.0001) {
                stream.setTrendScore(score);
                changed.add(stream);
            }
        }
        if (!changed.isEmpty()) {
            streamRepository.saveAll(changed);
        }
        log.debug("Trend scores recalculated: {} streams changed", changed.size());
    }
}
