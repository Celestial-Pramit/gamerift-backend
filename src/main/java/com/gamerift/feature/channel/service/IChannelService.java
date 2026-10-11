package com.gamerift.feature.channel.service;

import java.util.List;

import com.gamerift.feature.channel.dto.ChannelResponse;

/** What the channel feature can do. Every method knows WHICH user is asking (for the "following" flag). */
public interface IChannelService {

    List<ChannelResponse> getAll(String userId);

    ChannelResponse getById(String channelId, String userId);

    /** Only the channels this user follows. */
    List<ChannelResponse> getFollowing(String userId);

    /** Idempotent: following twice does nothing the second time. */
    ChannelResponse follow(String userId, String channelId);

    /** Idempotent: unfollowing a channel you do not follow does nothing. */
    ChannelResponse unfollow(String userId, String channelId);
}
