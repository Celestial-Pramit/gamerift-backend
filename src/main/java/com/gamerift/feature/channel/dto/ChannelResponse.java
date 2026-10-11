package com.gamerift.feature.channel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A channel as the frontend needs it. "following" tells whether the LOGGED-IN user follows it,
 * so the Follow button can show the right state without a second request.
 */
@Getter
@AllArgsConstructor
public class ChannelResponse {

    private String id;
    private String name;
    private long followersCount;
    // Short text for the UI, for example "42.1K"
    private String followersLabel;
    private boolean verified;
    private boolean pro;
    private boolean live;
    private String note;
    private boolean following;
}
