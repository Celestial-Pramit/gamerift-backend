package com.gamerift.feature.comment.api;

import java.util.List;

import com.gamerift.feature.comment.dto.CommentEditRequest;
import com.gamerift.feature.comment.dto.CommentRequest;
import com.gamerift.feature.comment.dto.CommentResponse;
import com.gamerift.feature.comment.service.ICommentService;
import com.gamerift.feature.stream.dto.LikeRequest;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for the comments under a stream player.
 * All URLs need a login (SecurityConfig).
 * Request flow of "post": Browser -> CommentController (@Valid) -> ICommentService
 *   -> CommentRepository -> MongoDB -> CommentResponse JSON (201).
 */
@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "Comments under a stream, with likes and replies")
public class CommentController {

    private final ICommentService commentService;

    public CommentController(ICommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "Top-level comments of a stream, newest first, each with its replies")
    public List<CommentResponse> getByStream(@RequestParam String streamId,
                                             @AuthenticationPrincipal AuthUser authUser) {
        return commentService.getByStream(streamId, authUser.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a comment (or a reply when parentId is set)")
    public CommentResponse create(@Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal AuthUser authUser) {
        return commentService.create(authUser.getId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit your own comment")
    public CommentResponse edit(@PathVariable String id, @Valid @RequestBody CommentEditRequest request,
                                @AuthenticationPrincipal AuthUser authUser) {
        return commentService.edit(authUser.getId(), id, request);
    }

    @PutMapping("/{id}/like")
    @Operation(summary = "Like or unlike a comment")
    public CommentResponse like(@PathVariable String id, @RequestBody LikeRequest request,
                                @AuthenticationPrincipal AuthUser authUser) {
        return commentService.setLike(authUser.getId(), id, request.isLiked());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete your own comment (replies and likes are removed too)")
    public void delete(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        commentService.delete(authUser.getId(), id);
    }
}