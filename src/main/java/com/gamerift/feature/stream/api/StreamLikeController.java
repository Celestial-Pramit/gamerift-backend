package com.gamerift.feature.stream.api;

import com.gamerift.feature.stream.dto.LikeRequest;
import com.gamerift.feature.stream.dto.LikeResponse;
import com.gamerift.feature.stream.service.IStreamLikeService;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The like button of a stream (needs a login). */
@RestController
@RequestMapping("/api/streams/{id}/like")
@Tag(name = "Stream likes", description = "Like and un-like a stream")
public class StreamLikeController {

    private final IStreamLikeService likeService;

    public StreamLikeController(IStreamLikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    @Operation(summary = "Did I like this stream? How many likes?")
    public LikeResponse getLike(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        return likeService.getLike(authUser.getId(), id);
    }

    @PutMapping
    @Operation(summary = "Like or un-like a stream")
    public LikeResponse setLike(@PathVariable String id, @RequestBody LikeRequest request,
                                @AuthenticationPrincipal AuthUser authUser) {
        return likeService.setLike(authUser.getId(), id, request.isLiked());
    }
}
