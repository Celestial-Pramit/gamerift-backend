package com.gamerift.feature.stream.service;

import com.gamerift.feature.stream.dto.LikeResponse;

/** The like button of a stream. */
public interface IStreamLikeService {

    LikeResponse getLike(String userId, String streamId);

    /** Idempotent: liking twice (or un-liking something not liked) changes nothing. */
    LikeResponse setLike(String userId, String streamId, boolean liked);
}
