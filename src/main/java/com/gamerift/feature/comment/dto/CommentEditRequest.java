package com.gamerift.feature.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/comments/{id}: the new comment text. */
@Getter
@Setter
@NoArgsConstructor
public class CommentEditRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = 500, message = "A comment can have at most 500 characters")
    private String text;
}