package com.gamerift.feature.comment.service;

import java.util.List;

import com.gamerift.feature.comment.dto.AdminCommentResponse;

/** Admin management of comments (list, delete anything, pin, lock). */
public interface IAdminCommentService {

    /** Every comment, newest first, with the title of its stream. */
    List<AdminCommentResponse> getAll();

    /** Deletes a comment wherever it is, including replies and likes. Throws 404 if missing. */
    void delete(String commentId);

    /** Pins or unpins a comment (pinned comments stay at the top of a stream). */
    AdminCommentResponse setPinned(String commentId, boolean pinned);

    /** Locks or unlocks a comment (locked comments accept no new replies). */
    AdminCommentResponse setLocked(String commentId, boolean locked);
}