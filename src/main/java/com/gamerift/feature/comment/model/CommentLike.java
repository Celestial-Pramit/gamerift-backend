package com.gamerift.feature.comment.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** One like on one comment (collection "comment_likes"). One row per user per comment. */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "comment_likes")
public class CommentLike {

    @Id
    private String id;

    private String commentId;
    private String userId;
}