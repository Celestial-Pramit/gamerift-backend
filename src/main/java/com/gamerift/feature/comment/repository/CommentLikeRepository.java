package com.gamerift.feature.comment.repository;

import java.util.Optional;

import com.gamerift.feature.comment.model.CommentLike;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentLikeRepository extends MongoRepository<CommentLike, String> {

    Optional<CommentLike> findByCommentIdAndUserId(String commentId, String userId);

    boolean existsByCommentIdAndUserId(String commentId, String userId);

    void deleteByCommentId(String commentId);
}