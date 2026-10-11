package com.gamerift.feature.comment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.comment.dto.CommentEditRequest;
import com.gamerift.feature.comment.dto.CommentRequest;
import com.gamerift.feature.comment.dto.CommentResponse;
import com.gamerift.feature.comment.model.Comment;
import com.gamerift.feature.comment.repository.CommentLikeRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the comment rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository likeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl service;

    private Comment comment(String id, String streamId, String userId, String username, String text, String parentId) {
        Comment c = new Comment();
        c.setId(id);
        c.setStreamId(streamId);
        c.setUserId(userId);
        c.setUsername(username);
        c.setText(text);
        c.setParentId(parentId);
        return c;
    }

    private User user(String id, String username, boolean banned) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setBanned(banned);
        return u;
    }

    private CommentRequest request(String streamId, String text, String parentId) {
        CommentRequest r = new CommentRequest();
        r.setStreamId(streamId);
        r.setText(text);
        r.setParentId(parentId);
        return r;
    }

    private CommentEditRequest edit(String text) {
        CommentEditRequest r = new CommentEditRequest();
        r.setText(text);
        return r;
    }

    @Test
    void getByStream_buildsTheCommentTree() {
        Comment top = comment("c1", "s1", "u1", "alice", "hello", null);
        top.setCreatedAt(Instant.now().minusSeconds(30));
        Comment reply = comment("c2", "s1", "u2", "bob", "hi there", "c1");
        Comment top2 = comment("c3", "s1", "u3", "carol", "yo", null);
        top2.setCreatedAt(Instant.now());
        when(commentRepository.findByStreamId("s1")).thenReturn(List.of(top, reply, top2));

        List<CommentResponse> result = service.getByStream("s1", "u9");

        assertEquals(2, result.size());
        assertEquals("carol", result.get(0).getUser());
        assertEquals("alice", result.get(1).getUser());
        assertEquals(1, result.get(1).getReplies().size());
        assertEquals("hi there", result.get(1).getReplies().get(0).getText());
    }

    @Test
    void create_cleansTheTextAndCopiesTheUsername() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = service.create("u1", request("s1", "  nice clip  ", null));

        assertEquals("alice", response.getUser());
        assertEquals("nice clip", response.getText());
        assertTrue(response.isMine());
    }

    @Test
    void create_bannedAuthor_throwsForbidden() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", true)));

        assertThrows(ForbiddenException.class, () -> service.create("u1", request("s1", "hi", null)));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void create_replyToUnknownComment_throwsBadRequest() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.create("u1", request("s1", "hi", "zzz")));
    }

    @Test
    void create_replyToACommentOfAnotherStream_throwsBadRequest() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment("c1", "s2", "u2", "bob", "hi", null)));

        assertThrows(BadRequestException.class, () -> service.create("u1", request("s1", "hi", "c1")));
    }

    @Test
    void create_replyToALockedComment_throwsBadRequest() {
        Comment parent = comment("c1", "s1", "u2", "bob", "hi", null);
        parent.setLocked(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(parent));

        assertThrows(BadRequestException.class, () -> service.create("u1", request("s1", "hi", "c1")));
    }

    @Test
    void create_replyTellsTheParentAuthor() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "bob", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment("c1", "s1", "u1", "alice", "hi", null)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("u2", request("s1", "hello alice", "c1"));

        verify(notificationService).send("u1", "reply", "alice replied to your comment", "/app/stream/s1");
    }

    @Test
    void create_replyToYourself_doesNotNotify() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment("c1", "s1", "u1", "alice", "hi", null)));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("u1", request("s1", "adding on", "c1"));

        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    @Test
    void setLike_liked_trueAddsExactlyOneLike() {
        Comment c = comment("c1", "s1", "u1", "alice", "hi", null);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "bob", false)));
        when(likeRepository.existsByCommentIdAndUserId("c1", "u2")).thenReturn(false).thenReturn(true);

        CommentResponse response = service.setLike("u2", "c1", true);

        verify(likeRepository).save(any());
        assertEquals(1, c.getLikesCount());
        assertTrue(response.isLiked());
    }

    @Test
    void setLike_unliked_removesTheLikeRow() {
        Comment c = comment("c1", "s1", "u1", "alice", "hi", null);
        c.setLikesCount(3);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "bob", false)));
        when(likeRepository.existsByCommentIdAndUserId("c1", "u2")).thenReturn(true).thenReturn(false);
        when(likeRepository.findByCommentIdAndUserId("c1", "u2")).thenReturn(Optional.of(
                new com.gamerift.feature.comment.model.CommentLike()));

        CommentResponse response = service.setLike("u2", "c1", false);

        verify(likeRepository).delete(any());
        assertEquals(2, c.getLikesCount());
        assertFalse(response.isLiked());
    }

    @Test
    void delete_removesTheCommentWithItsRepliesAndLikes() {
        Comment top = comment("c1", "s1", "u1", "alice", "hi", null);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(top));
        when(commentRepository.findByParentId("c1")).thenReturn(List.of(comment("r1", "s1", "u2", "bob", "yo", "c1")));

        service.delete("u1", "c1");

        verify(likeRepository).deleteByCommentId("r1");
        verify(commentRepository).deleteByParentId("c1");
        verify(likeRepository).deleteByCommentId("c1");
        verify(commentRepository).delete(top);
    }

    @Test
    void delete_anotherUsersComment_throwsForbidden() {
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment("c1", "s1", "u1", "alice", "hi", null)));

        assertThrows(ForbiddenException.class, () -> service.delete("u2", "c1"));
    }

    @Test
    void edit_cleansTheTextAndMarksItEdited() {
        Comment c = comment("c1", "s1", "u1", "alice", "old text", null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(c));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = service.edit("u1", "c1", edit("  new text  "));

        assertEquals("new text", response.getText());
        assertTrue(response.isEdited());
    }

    @Test
    void edit_anotherUsersComment_throwsForbidden() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "bob", false)));
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment("c1", "s1", "u1", "alice", "hi", null)));

        assertThrows(ForbiddenException.class, () -> service.edit("u2", "c1", edit("hacked")));
    }

    @Test
    void edit_unknownComment_throwsNotFound() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "alice", false)));
        when(commentRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.edit("u1", "zzz", edit("hi")));
    }
}