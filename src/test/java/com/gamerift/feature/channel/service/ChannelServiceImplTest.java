package com.gamerift.feature.channel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.dto.ChannelResponse;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.model.Follow;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the channel / follow rules (repositories are fakes). */
@ExtendWith(MockitoExtension.class)
class ChannelServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private ChannelServiceImpl channelService;

    private Channel channel(String id, String name, long followers) {
        Channel c = new Channel();
        c.setId(id);
        c.setName(name);
        c.setFollowersCount(followers);
        return c;
    }

    private Follow follow(String userId, String channelId) {
        Follow f = new Follow();
        f.setUserId(userId);
        f.setChannelId(channelId);
        return f;
    }

    @Test
    void getAll_marksOnlyTheFollowedChannels() {
        when(channelRepository.findAll()).thenReturn(List.of(channel("c1", "NightRaid", 148000), channel("c2", "Rozu", 42100)));
        when(followRepository.findByUserId("u1")).thenReturn(List.of(follow("u1", "c2")));

        List<ChannelResponse> result = channelService.getAll("u1");

        assertFalse(result.get(0).isFollowing());
        assertTrue(result.get(1).isFollowing());
        assertEquals("148K", result.get(0).getFollowersLabel());
        assertEquals("42.1K", result.get(1).getFollowersLabel());
    }

    @Test
    void follow_newFollow_savesItAndAddsOneFollower() {
        Channel c = channel("c1", "NightRaid", 10);
        when(channelRepository.findById("c1")).thenReturn(Optional.of(c));
        when(followRepository.existsByUserIdAndChannelId("u1", "c1")).thenReturn(false);

        ChannelResponse response = channelService.follow("u1", "c1");

        verify(followRepository).save(any(Follow.class));
        assertEquals(11, c.getFollowersCount());
        verify(channelRepository).save(c);
        assertTrue(response.isFollowing());
    }

    @Test
    void follow_alreadyFollowing_changesNothing() {
        Channel c = channel("c1", "NightRaid", 10);
        when(channelRepository.findById("c1")).thenReturn(Optional.of(c));
        when(followRepository.existsByUserIdAndChannelId("u1", "c1")).thenReturn(true);

        channelService.follow("u1", "c1");

        verify(followRepository, never()).save(any(Follow.class));
        assertEquals(10, c.getFollowersCount());
    }

    @Test
    void follow_unknownChannel_throwsNotFound() {
        when(channelRepository.findById("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> channelService.follow("u1", "nope"));
    }

    @Test
    void unfollow_existingFollow_deletesItAndRemovesOneFollower() {
        Channel c = channel("c1", "NightRaid", 10);
        Follow f = follow("u1", "c1");
        when(channelRepository.findById("c1")).thenReturn(Optional.of(c));
        when(followRepository.findByUserIdAndChannelId("u1", "c1")).thenReturn(Optional.of(f));

        ChannelResponse response = channelService.unfollow("u1", "c1");

        verify(followRepository).delete(f);
        assertEquals(9, c.getFollowersCount());
        assertFalse(response.isFollowing());
    }

    @Test
    void unfollow_whenNotFollowing_changesNothing() {
        Channel c = channel("c1", "NightRaid", 10);
        when(channelRepository.findById("c1")).thenReturn(Optional.of(c));
        when(followRepository.findByUserIdAndChannelId("u1", "c1")).thenReturn(Optional.empty());

        channelService.unfollow("u1", "c1");

        verify(followRepository, never()).delete(any(Follow.class));
        assertEquals(10, c.getFollowersCount());
    }

    @Test
    void getFollowing_returnsOnlyTheFollowedChannels() {
        when(followRepository.findByUserId("u1")).thenReturn(List.of(follow("u1", "c2")));
        when(channelRepository.findAllById(any())).thenReturn(List.of(channel("c2", "Rozu", 42100)));

        List<ChannelResponse> result = channelService.getFollowing("u1");

        assertEquals(1, result.size());
        assertEquals("Rozu", result.get(0).getName());
        assertTrue(result.get(0).isFollowing());
    }

    @Test
    void followersLabel_isShortAndReadable() {
        assertEquals("999", ChannelMapper.formatFollowers(999));
        assertEquals("9.6K", ChannelMapper.formatFollowers(9600));
        assertEquals("1.5M", ChannelMapper.formatFollowers(1_500_000));
    }
}
