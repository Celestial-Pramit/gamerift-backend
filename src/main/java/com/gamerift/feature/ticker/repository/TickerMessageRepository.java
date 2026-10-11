package com.gamerift.feature.ticker.repository;

import com.gamerift.feature.ticker.model.TickerMessage;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TickerMessageRepository extends MongoRepository<TickerMessage, String> {
}
