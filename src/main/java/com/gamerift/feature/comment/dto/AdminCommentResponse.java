package com.gamerift.feature.comment.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the admin "Comments" screen. stream = the title of the stream ("" when missing). */
@Getter
@Setter
@NoArgsConstructor
public class AdminCommentResponse {

    private String id;
    private String user;
    private String text;
    private String stream;
    private boolean pinned;
    private boolean locked;
    private Instant createdAt;
}