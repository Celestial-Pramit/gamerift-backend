package com.gamerift.feature.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of POST /api/comments. parentId is null for a top-level comment. */
@Getter
@Setter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Stream is required")
    private String streamId;

    @NotBlank(message = "Comment text is required")
    @Size(max = 500, message = "A comment can have at most 500 characters")
    private String text;

    private String parentId;
}