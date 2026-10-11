package com.gamerift.feature.event.repository;

import java.time.Instant;
import java.util.List;

import com.gamerift.feature.event.model.GameEvent;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameEventRepository extends MongoRepository<GameEvent, String> {

    /** Events starting inside the given window (used by the reminder job). */
    List<GameEvent> findByStartsAtBetween(Instant from, Instant to);
}
