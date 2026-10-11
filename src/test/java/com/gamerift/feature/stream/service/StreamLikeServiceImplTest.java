package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.gamerift.feature.stream.dto.LikeResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamLike;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.stream.repository.StreamLikeRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamLikeServiceImplTest {

    @Mock
    private LiveStreamRepository streamRepository;
    @Mock
    private StreamLikeRepository likeRepository;

    @InjectMocks
    private StreamLikeServiceImpl likeService;

    private LiveStream stream(boolean published, int likes) {
        LiveStream s = new LiveStream();
        s.setId("s1");
        s.setPublished(published);
        s.setLikesCount(likes);
        return s;
    }

    @Test
    void like_new_savesTheLikeAndAddsOne() {
        LiveStream s = stream(true, 10);
        when(streamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(false);

        LikeResponse response = likeService.setLike("u1", "s1", true);

        verify(likeRepository).save(any(StreamLike.class));
        assertEquals(11, s.getLikesCount());
        assertTrue(response.isLiked());
        assertEquals(11, response.getLikesCount());
    }

    @Test
    void like_twice_changesNothing() {
        LiveStream s = stream(true, 10);
        when(streamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(true);

        likeService.setLike("u1", "s1", true);

        verify(likeRepository, never()).save(any(StreamLike.class));
        assertEquals(10, s.getLikesCount());
    }

    @Test
    void unlike_removesTheLikeAndSubtractsOne() {
        LiveStream s = stream(true, 10);
        when(streamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(true);

        LikeResponse response = likeService.setLike("u1", "s1", false);

        verify(likeRepository).deleteByUserIdAndStreamId("u1", "s1");
        assertEquals(9, s.getLikesCount());
        assertFalse(response.isLiked());
    }

    @Test
    void unlike_whenNotLiked_changesNothing() {
        LiveStream s = stream(true, 10);
        when(streamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(false);

        likeService.setLike("u1", "s1", false);

        assertEquals(10, s.getLikesCount());
    }

    @Test
    void likeCount_neverGoesBelowZero() {
        LiveStream s = stream(true, 0);
        when(streamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(true);

        likeService.setLike("u1", "s1", false);

        assertEquals(0, s.getLikesCount());
    }

    @Test
    void like_aDraft_looksLikeItDoesNotExist() {
        when(streamRepository.findById("s1")).thenReturn(Optional.of(stream(false, 0)));

        assertThrows(NotFoundException.class, () -> likeService.setLike("u1", "s1", true));
    }

    @Test
    void getLike_returnsTheStateAndTheCount() {
        when(streamRepository.findById("s1")).thenReturn(Optional.of(stream(true, 42)));
        when(likeRepository.existsByUserIdAndStreamId("u1", "s1")).thenReturn(true);

        LikeResponse response = likeService.getLike("u1", "s1");

        assertTrue(response.isLiked());
        assertEquals(42, response.getLikesCount());
    }
}
