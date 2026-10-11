package com.gamerift.feature.channel.service;

import java.util.Locale;

import com.gamerift.feature.channel.dto.ChannelResponse;
import com.gamerift.feature.channel.model.Channel;

/** Copies a Channel (model) into a ChannelResponse (DTO). */
public final class ChannelMapper {

    private ChannelMapper() {
    }

    public static ChannelResponse toResponse(Channel channel, boolean following) {
        return new ChannelResponse(channel.getId(), channel.getName(), channel.getFollowersCount(),
                formatFollowers(channel.getFollowersCount()), channel.isVerified(), channel.isPro(),
                channel.isLive(), channel.getNote(), following);
    }

    /** 148000 -> "148K", 42100 -> "42.1K", 1500000 -> "1.5M", 999 -> "999". */
    static String formatFollowers(long count) {
        if (count >= 1_000_000) {
            return oneDecimal(count / 1_000_000.0) + "M";
        }
        if (count >= 1_000) {
            return oneDecimal(count / 1_000.0) + "K";
        }
        return String.valueOf(count);
    }

    private static String oneDecimal(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
