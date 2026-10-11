package com.gamerift.feature.comment.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gamerift.feature.comment.dto.CommentEditRequest;
import com.gamerift.feature.comment.dto.CommentRequest;
import com.gamerift.feature.comment.dto.CommentResponse;
import com.gamerift.feature.comment.model.Comment;
import com.gamerift.feature.comment.model.CommentLike;
import com.gamerift.feature.comment.repository.CommentLikeRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for comments (layer: service).
 *  - banned accounts cannot post or like,
 *  - you can only delete your own comment (the admin can delete anything, see AdminCommentServiceImpl),
 *  - deleting a comment also deletes its replies and every like on it,
 *  - a reply tells the author of the parent comment, but only when they have Replies alerts on.
 */
@Service
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;

    public CommentServiceImpl(CommentRepository commentRepository, CommentLikeRepository likeRepository,
                              UserRepository userRepository, INotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<CommentResponse> getByStream(String streamId, String userId) {
        // 1. read every comment of the stream once (it stays a flat list in the database)
        List<Comment> comments = commentRepository.findByStreamId(streamId);
        Map<String, CommentResponse> tops = new LinkedHashMap<>();
        List<CommentResponse> result = new ArrayList<>();

        // 2. first pass: every top-level comment becomes a row
        for (Comment c : comments) {
            if (c.getParentId() == null || c.getParentId().isBlank()) {
                CommentResponse r = at(c, userId);
                tops.put(c.getId(), r);
                result.add(r);
            }
        }

        // 3. second pass: attach the replies to their parent
        for (Comment c : comments) {
            if (c.getParentId() != null && !c.getParentId().isBlank()) {
                CommentResponse parent = tops.get(c.getParentId());
                if (parent != null) {
                    parent.getReplies().add(at(c, userId));
                }
            }
        }

        // 4. newest top-level comments first; replies stay oldest first
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return result;
    }

    @Override
    public CommentResponse create(String userId, CommentRequest request) {
        // 1. the account must be allowed to post
        User author = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (author.isBanned()) {
            throw new ForbiddenException("You are banned from commenting");
        }

        // 2. check the reply target (if any) belongs to the same stream
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BadRequestException("The comment you reply to does not exist"));
            if (!parent.getStreamId().equals(request.getStreamId())) {
                throw new BadRequestException("The comment you reply to is not on this stream");
            }
            if (parent.isLocked()) {
                throw new BadRequestException("This comment is locked");
            }
        }

        // 3. clean the text
        String text = request.getText().trim();

        // 4. build and save
        Comment comment = new Comment();
        comment.setStreamId(request.getStreamId().trim());
        comment.setUserId(userId);
        comment.setUsername(author.getUsername());
        comment.setText(text);
        comment.setParentId(request.getParentId());
        commentRepository.save(comment);

        // 5. reply: notify the author of the parent comment
        notifyParentAuthor(request.getStreamId().trim(), request.getParentId(), author);

        return at(comment, userId);
    }

    @Override
    public CommentResponse edit(String userId, String commentId, CommentEditRequest request) {
        // 1. the account must be allowed to comment
        User editor = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (editor.isBanned()) {
            throw new ForbiddenException("You are banned from commenting");
        }
        // 2. the comment must exist and be owned by this user
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own comments");
        }
        // 3. clean the text and save
        comment.setText(request.getText().trim());
        comment.setEdited(true);
        return at(commentRepository.save(comment), userId);
    }

    @Override
    public CommentResponse setLike(String userId, String commentId, boolean liked) {
        // 1. the comment must exist
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        // 2. banned accounts cannot like
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.isBanned()) {
            throw new ForbiddenException("You are banned from liking comments");
        }

        // 3. remove or add exactly one like row
        boolean exists = likeRepository.existsByCommentIdAndUserId(commentId, userId);
        if (liked && !exists) {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            likeRepository.save(like);
            comment.setLikesCount(comment.getLikesCount() + 1);
        }
        if (!liked && exists) {
            // deleteByCommentIdAndUserId would be nicer; a derived delete needs a void method on the repo
            likeRepository.findByCommentIdAndUserId(commentId, userId)
                    .ifPresent(likeRepository::delete);
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        }
        commentRepository.save(comment);
        return at(comment, userId);
    }

    @Override
    public void delete(String userId, String commentId) {
        // 1. the comment must exist
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        // 2. you can only delete your own comment
        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own comments");
        }
        deleteWithReplies(comment);
    }

    // likes of a comment and of every reply go away before the comment itself
    private void deleteWithReplies(Comment comment) {
        for (Comment reply : commentRepository.findByParentId(comment.getId())) {
            likeRepository.deleteByCommentId(reply.getId());
        }
        commentRepository.deleteByParentId(comment.getId());
        likeRepository.deleteByCommentId(comment.getId());
        commentRepository.delete(comment);
    }

    private void notifyParentAuthor(String streamId, String parentId, User author) {
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        Comment parent = commentRepository.findById(parentId).orElse(null);
        if (parent == null || parent.getUserId().equals(author.getId())) {
            return;
        }
        notificationService.send(parent.getUserId(), "reply",
                parent.getUsername() + " replied to your comment", "/app/stream/" + streamId);
    }

    private CommentResponse at(Comment c, String userId) {
        boolean liked = likeRepository.existsByCommentIdAndUserId(c.getId(), userId);
        boolean mine = c.getUserId().equals(userId);
        boolean pro = userRepository.findById(c.getUserId()).map(User::isPro).orElse(false);
        return CommentMapper.toResponse(c, liked, mine, pro);
    }
}