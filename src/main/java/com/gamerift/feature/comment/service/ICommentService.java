package com.gamerift.feature.comment.service;

import java.util.List;

import com.gamerift.feature.comment.dto.CommentEditRequest;
import com.gamerift.feature.comment.dto.CommentRequest;
import com.gamerift.feature.comment.dto.CommentResponse;

/** What the stream page can do with comments (post, like, delete your own). */
public interface ICommentService {

    /** All top-level comments of a stream, newest first, each with its replies. */
    List<CommentResponse> getByStream(String streamId, String userId);

    /** Posts a comment (or a reply when parentId is set). Banned users are rejected. */
    CommentResponse create(String userId, CommentRequest request);

    /** Edits the user's own comment. Banned users are rejected. */
    CommentResponse edit(String userId, String commentId, CommentEditRequest request);

    /** Likes or unlikes a comment. Banned users are rejected. */
    CommentResponse setLike(String userId, String commentId, boolean liked);

    /** Deletes the user's own comment (replies and likes of it are removed too). */
    void delete(String userId, String commentId);
}