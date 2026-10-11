package com.gamerift.feature.comment.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One comment (collection "comments").
 * parentId is null for a top-level comment and set for a reply.
 * pinned/locked are admin flags: pinned shows first, locked blocks new replies.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    private String streamId;
    private String userId;
    // copy of the author's username so list pages need no second query
    private String username;
    private String text;
    // null = top-level comment, otherwise the id of the comment this replies to
    private String parentId;
    private boolean pinned;
    private boolean locked;
    private boolean edited;
    private int likesCount;
    private Instant createdAt = Instant.now();
}