package com.gamerift.feature.stream.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * "User X likes stream Y" (collection "stream_likes").
 * The unique compound index guarantees one like per user per stream.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "stream_likes")
@CompoundIndex(name = "user_stream", def = "{'userId': 1, 'streamId': 1}", unique = true)
public class StreamLike {

    @Id
    private String id;

    private String userId;
    private String streamId;
    private Instant createdAt = Instant.now();
}
