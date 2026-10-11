package com.gamerift.feature.stream.service;

import com.gamerift.feature.stream.dto.LikeResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamLike;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.stream.repository.StreamLikeRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules of likes (layer: service).
 *  - only published streams can be liked (a draft looks like "does not exist"),
 *  - one like per user per stream (also protected by a unique index),
 *  - the stream keeps a likesCount that goes up and down (never below 0); the trend job uses it.
 */
@Service
public class StreamLikeServiceImpl implements IStreamLikeService {

    private final LiveStreamRepository streamRepository;
    private final StreamLikeRepository likeRepository;

    public StreamLikeServiceImpl(LiveStreamRepository streamRepository, StreamLikeRepository likeRepository) {
        this.streamRepository = streamRepository;
        this.likeRepository = likeRepository;
    }

    @Override
    public LikeResponse getLike(String userId, String streamId) {
        LiveStream stream = findPublished(streamId);
        boolean liked = likeRepository.existsByUserIdAndStreamId(userId, streamId);
        return new LikeResponse(liked, stream.getLikesCount());
    }

    @Override
    public LikeResponse setLike(String userId, String streamId, boolean liked) {
        LiveStream stream = findPublished(streamId);
        boolean exists = likeRepository.existsByUserIdAndStreamId(userId, streamId);

        if (liked && !exists) {
            StreamLike like = new StreamLike();
            like.setUserId(userId);
            like.setStreamId(streamId);
            likeRepository.save(like);

            stream.setLikesCount(stream.getLikesCount() + 1);
            streamRepository.save(stream);
        }
        if (!liked && exists) {
            likeRepository.deleteByUserIdAndStreamId(userId, streamId);

            stream.setLikesCount(Math.max(0, stream.getLikesCount() - 1));
            streamRepository.save(stream);
        }
        return new LikeResponse(liked, stream.getLikesCount());
    }

    private LiveStream findPublished(String streamId) {
        LiveStream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new NotFoundException("Stream not found"));
        if (!stream.isPublished()) {
            throw new NotFoundException("Stream not found");
        }
        return stream;
    }
}
