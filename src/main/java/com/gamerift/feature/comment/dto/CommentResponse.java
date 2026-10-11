package com.gamerift.feature.comment.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A comment as the stream page renders it.
 * liked = the logged-in user liked it, mine = the logged-in user wrote it (delete button).
 * replies only exists on top-level comments.
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentResponse {

    private String id;
    private String streamId;
    private String user;
    private boolean pro;
    private String text;
    private String parentId;
    private boolean pinned;
    private boolean locked;
    private boolean edited;
    private int likesCount;
    private boolean liked;
    private boolean mine;
    private Instant createdAt;
    private List<CommentResponse> replies = new ArrayList<>();
}