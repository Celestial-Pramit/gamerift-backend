package com.gamerift.feature.channel.repository;

import java.util.Optional;

import com.gamerift.feature.channel.model.Channel;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChannelRepository extends MongoRepository<Channel, String> {

    Optional<Channel> findByName(String name);
}
