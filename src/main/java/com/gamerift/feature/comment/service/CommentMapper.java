package com.gamerift.feature.comment.service;

import com.gamerift.feature.comment.dto.CommentResponse;
import com.gamerift.feature.comment.model.Comment;

/** Copies a Comment into a CommentResponse. Pure mapping, no logic. */
public final class CommentMapper {

    private CommentMapper() {
    }

    static CommentResponse toResponse(Comment comment, boolean liked, boolean mine, boolean pro) {
        CommentResponse r = new CommentResponse();
        r.setId(comment.getId());
        r.setStreamId(comment.getStreamId());
        r.setUser(comment.getUsername());
        r.setPro(pro);
        r.setText(comment.getText());
        r.setParentId(comment.getParentId());
        r.setPinned(comment.isPinned());
        r.setLocked(comment.isLocked());
        r.setEdited(comment.isEdited());
        r.setLikesCount(comment.getLikesCount());
        r.setLiked(liked);
        r.setMine(mine);
        r.setCreatedAt(comment.getCreatedAt());
        return r;
    }
}