package com.gamerift.feature.channel.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gamerift.feature.channel.dto.ChannelResponse;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.model.Follow;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules of channels and following (layer: service).
 *  - a channel must exist before it can be followed,
 *  - following is idempotent (no double follow, no double count),
 *  - the channel's followersCount goes up on follow and down on unfollow (never below 0).
 * (Two perfectly simultaneous requests are stopped by the unique index of "follows".)
 */
@Service
public class ChannelServiceImpl implements IChannelService {

    private final ChannelRepository channelRepository;
    private final FollowRepository followRepository;

    public ChannelServiceImpl(ChannelRepository channelRepository, FollowRepository followRepository) {
        this.channelRepository = channelRepository;
        this.followRepository = followRepository;
    }

    @Override
    public List<ChannelResponse> getAll(String userId) {
        Set<String> followedIds = followedChannelIds(userId);
        List<ChannelResponse> result = new ArrayList<>();
        for (Channel channel : channelRepository.findAll()) {
            result.add(ChannelMapper.toResponse(channel, followedIds.contains(channel.getId())));
        }
        return result;
    }

    @Override
    public ChannelResponse getById(String channelId, String userId) {
        Channel channel = findChannel(channelId);
        boolean following = followRepository.existsByUserIdAndChannelId(userId, channelId);
        return ChannelMapper.toResponse(channel, following);
    }

    @Override
    public List<ChannelResponse> getFollowing(String userId) {
        Set<String> followedIds = followedChannelIds(userId);
        List<ChannelResponse> result = new ArrayList<>();
        for (Channel channel : channelRepository.findAllById(followedIds)) {
            result.add(ChannelMapper.toResponse(channel, true));
        }
        return result;
    }

    @Override
    public ChannelResponse follow(String userId, String channelId) {
        // 1. the channel must exist
        Channel channel = findChannel(channelId);
        // 2. only when not following yet: save the follow and add one follower
        if (!followRepository.existsByUserIdAndChannelId(userId, channelId)) {
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setChannelId(channelId);
            followRepository.save(follow);

            channel.setFollowersCount(channel.getFollowersCount() + 1);
            channelRepository.save(channel);
        }
        return ChannelMapper.toResponse(channel, true);
    }

    @Override
    public ChannelResponse unfollow(String userId, String channelId) {
        Channel channel = findChannel(channelId);
        Optional<Follow> existing = followRepository.findByUserIdAndChannelId(userId, channelId);
        if (existing.isPresent()) {
            followRepository.delete(existing.get());

            channel.setFollowersCount(Math.max(0, channel.getFollowersCount() - 1));
            channelRepository.save(channel);
        }
        return ChannelMapper.toResponse(channel, false);
    }

    private Channel findChannel(String channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found"));
    }

    private Set<String> followedChannelIds(String userId) {
        Set<String> ids = new HashSet<>();
        for (Follow follow : followRepository.findByUserId(userId)) {
            ids.add(follow.getChannelId());
        }
        return ids;
    }
}
