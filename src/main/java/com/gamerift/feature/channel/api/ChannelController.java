package com.gamerift.feature.channel.api;

import java.util.List;

import com.gamerift.feature.channel.dto.ChannelResponse;
import com.gamerift.feature.channel.service.IChannelService;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for channels and follow / unfollow.
 * All URLs need a login (SecurityConfig: anyRequest().authenticated()).
 *
 * Request flow of "Follow": Browser -> ChannelController -> IChannelService (ChannelServiceImpl)
 *   -> FollowRepository + ChannelRepository -> MongoDB -> ChannelResponse JSON.
 * Admin create/update/delete of channels arrives with the admin APIs in Phase 9.
 */
@RestController
@RequestMapping("/api/channels")
@Tag(name = "Channels", description = "Channels and follow / unfollow")
public class ChannelController {

    private final IChannelService channelService;

    public ChannelController(IChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    @Operation(summary = "All channels, each with the 'following' flag of the logged-in user")
    public List<ChannelResponse> getAll(@AuthenticationPrincipal AuthUser authUser) {
        return channelService.getAll(authUser.getId());
    }

    // Declared before "/{id}" for clarity: the fixed word "following" is not an id
    @GetMapping("/following")
    @Operation(summary = "Only the channels the logged-in user follows")
    public List<ChannelResponse> getFollowing(@AuthenticationPrincipal AuthUser authUser) {
        return channelService.getFollowing(authUser.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One channel (404 if it does not exist)")
    public ChannelResponse getById(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        return channelService.getById(id, authUser.getId());
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "Follow a channel")
    public ChannelResponse follow(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        return channelService.follow(authUser.getId(), id);
    }

    @DeleteMapping("/{id}/follow")
    @Operation(summary = "Unfollow a channel")
    public ChannelResponse unfollow(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        return channelService.unfollow(authUser.getId(), id);
    }
}
