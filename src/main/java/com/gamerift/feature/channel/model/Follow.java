package com.gamerift.feature.channel.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * "User X follows channel Y" (collection "follows").
 * The unique compound index makes it impossible to store the same follow twice,
 * even if two requests arrive at the same moment.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "follows")
@CompoundIndex(name = "user_channel", def = "{'userId': 1, 'channelId': 1}", unique = true)
public class Follow {

    @Id
    private String id;

    private String userId;
    private String channelId;
    private Instant createdAt = Instant.now();
}
