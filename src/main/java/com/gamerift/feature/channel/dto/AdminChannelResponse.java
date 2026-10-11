package com.gamerift.feature.channel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A channel as the admin panel table sees it. */
@Getter
@Setter
@NoArgsConstructor
public class AdminChannelResponse {

    private String id;
    private String name;
    private long followersCount;
    private boolean verified;
    private boolean pro;
    private boolean live;
    private String note;
}
