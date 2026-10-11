package com.gamerift.feature.comment.repository;

import java.util.List;

import com.gamerift.feature.comment.model.Comment;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, String> {

    /** Every comment of one stream, oldest first (the service builds the tree and sorts). */
    List<Comment> findByStreamId(String streamId);

    /** The direct replies of one comment, oldest first. */
    List<Comment> findByParentId(String parentId);

    /** Used when deleting a comment: are there replies to remove first? */
    boolean existsByParentId(String parentId);

    void deleteByParentId(String parentId);
}