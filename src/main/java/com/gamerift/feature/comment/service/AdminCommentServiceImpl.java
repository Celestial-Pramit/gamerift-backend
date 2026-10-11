package com.gamerift.feature.comment.service;

import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.comment.dto.AdminCommentResponse;
import com.gamerift.feature.comment.model.Comment;
import com.gamerift.feature.comment.repository.CommentLikeRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for the admin "Comments" screen (layer: service).
 * The stream title is fetched for display only; missing streams show an empty string.
 */
@Service
public class AdminCommentServiceImpl implements IAdminCommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository likeRepository;
    private final LiveStreamRepository streamRepository;

    public AdminCommentServiceImpl(CommentRepository commentRepository, CommentLikeRepository likeRepository,
                                   LiveStreamRepository streamRepository) {
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.streamRepository = streamRepository;
    }

    @Override
    public List<AdminCommentResponse> getAll() {
        List<Comment> comments = new ArrayList<>(commentRepository.findAll());
        comments.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        List<AdminCommentResponse> result = new ArrayList<>();
        for (Comment c : comments) {
            result.add(toResponse(c));
        }
        return result;
    }

    @Override
    public void delete(String commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment not found");
        }
        // 1. remove the replies of the comment and their likes
        for (Comment reply : commentRepository.findByParentId(commentId)) {
            likeRepository.deleteByCommentId(reply.getId());
        }
        commentRepository.deleteByParentId(commentId);
        // 2. remove the comment itself
        likeRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public AdminCommentResponse setPinned(String commentId, boolean pinned) {
        return toResponse(update(commentId, "pinned", pinned));
    }

    @Override
    public AdminCommentResponse setLocked(String commentId, boolean locked) {
        return toResponse(update(commentId, "locked", locked));
    }

    private Comment update(String commentId, String field, boolean value) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if ("pinned".equals(field)) {
            comment.setPinned(value);
        } else {
            comment.setLocked(value);
        }
        return commentRepository.save(comment);
    }

    private AdminCommentResponse toResponse(Comment c) {
        AdminCommentResponse r = new AdminCommentResponse();
        r.setId(c.getId());
        r.setUser(c.getUsername());
        r.setText(c.getText());
        r.setPinned(c.isPinned());
        r.setLocked(c.isLocked());
        r.setCreatedAt(c.getCreatedAt());
        // the stream title makes the row readable; missing stream -> ""
        r.setStream(streamRepository.findById(c.getStreamId()).map(LiveStream::getTitle).orElse(""));
        return r;
    }
}