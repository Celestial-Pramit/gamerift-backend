package com.gamerift.feature.channel.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A streamer channel that users can follow (collection "channels"). */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "channels")
public class Channel {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private long followersCount;
    private boolean verified;
    private boolean pro;
    private boolean live;
    // Short text such as "GRAND FINALS · MAP 3"
    private String note;
}
