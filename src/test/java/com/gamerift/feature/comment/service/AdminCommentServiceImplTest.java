package com.gamerift.feature.comment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.comment.dto.AdminCommentResponse;
import com.gamerift.feature.comment.model.Comment;
import com.gamerift.feature.comment.repository.CommentLikeRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin comment rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminCommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository likeRepository;
    @Mock
    private LiveStreamRepository streamRepository;

    @InjectMocks
    private AdminCommentServiceImpl service;

    private Comment comment(String id, String streamId, String username, String text) {
        Comment c = new Comment();
        c.setId(id);
        c.setStreamId(streamId);
        c.setUsername(username);
        c.setText(text);
        return c;
    }

    @Test
    void getAll_newestFirst_andShowsTheStreamTitle() {
        Comment old = comment("c1", "s1", "alice", "old");
        old.setCreatedAt(Instant.now().minusSeconds(30));
        Comment fresh = comment("c2", "s1", "bob", "fresh");
        fresh.setCreatedAt(Instant.now());
        when(commentRepository.findAll()).thenReturn(List.of(old, fresh));
        LiveStream stream = new LiveStream();
        stream.setTitle("Grand Finals");
        when(streamRepository.findById("s1")).thenReturn(Optional.of(stream));

        List<AdminCommentResponse> result = service.getAll();

        assertEquals(2, result.size());
        assertEquals("c2", result.get(0).getId());
        assertEquals("Grand Finals", result.get(0).getStream());
    }

    @Test
    void delete_removesTheLikesOfRepliesThenTheRepliesThenTheComment() {
        when(commentRepository.existsById("c1")).thenReturn(true);
        when(commentRepository.findByParentId("c1")).thenReturn(List.of(comment("r1", "s1", "bob", "yo")));

        service.delete("c1");

        verify(likeRepository).deleteByCommentId("r1");
        verify(commentRepository).deleteByParentId("c1");
        verify(likeRepository).deleteByCommentId("c1");
        verify(commentRepository).deleteById("c1");
    }

    @Test
    void delete_unknownComment_throwsNotFound() {
        when(commentRepository.existsById("zzz")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.delete("zzz"));
    }

    @Test
    void setPinned_flipsThePinnedFlag() {
        Comment c = comment("c1", "s1", "alice", "hi");
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCommentResponse response = service.setPinned("c1", true);

        assertTrue(response.isPinned());
        verify(commentRepository).save(c);
    }
}