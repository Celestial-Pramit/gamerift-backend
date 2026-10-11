package com.gamerift.shared.security;

import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.MessageDeliveryException;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for the STOMP CONNECT JWT interceptor. */
@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtChannelInterceptor interceptor;

    /** Test Principal implementation matching the one in JwtChannelInterceptor. */
    private static class TestPrincipal implements Principal {
        private final String userId;
        TestPrincipal(String userId) { this.userId = userId; }
        @Override public String getName() { return userId; }
    }

    private Message<byte[]> connectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(new TestPrincipal("user-123"));
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> message(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> messageWithPrincipal(StompCommand command, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void validToken_setsPrincipalName_userId() {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setRole(Role.VIEWER);

        when(jwtUtil.validateAndGetUserId("good-token")).thenReturn("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        Message<?> result = interceptor.preSend(connectMessage("Bearer good-token"), mock(MessageChannel.class));

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertEquals("user-123", accessor.getUser().getName());
        verify(userRepository).findById("user-123");
    }

    @Test
    void missingHeader_rejected() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectMessage(null), mock(MessageChannel.class)));
    }

    @Test
    void missingBearerPrefix_rejected() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectMessage("Token abc"), mock(MessageChannel.class)));
    }

    @Test
    void invalidToken_rejected() {
        when(jwtUtil.validateAndGetUserId("bad-token")).thenReturn(null);

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectMessage("Bearer bad-token"), mock(MessageChannel.class)));
    }

    @Test
    void bannedUser_rejected() {
        User user = new User();
        user.setId("banned-user");
        user.setRole(Role.VIEWER);
        user.setBanned(true);

        when(jwtUtil.validateAndGetUserId("banned-token")).thenReturn("banned-user");
        when(userRepository.findById("banned-user")).thenReturn(Optional.of(user));

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectMessage("Bearer banned-token"), mock(MessageChannel.class)));
    }

    @Test
    void unknownUser_rejected() {
        when(jwtUtil.validateAndGetUserId("unknown-token")).thenReturn("unknown-user");
        when(userRepository.findById("unknown-user")).thenReturn(Optional.empty());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectMessage("Bearer unknown-token"), mock(MessageChannel.class)));
    }

    @Test
    void subscribe_userQueue_allowed() {
        Message<?> result = interceptor.preSend(subscribeMessage("/user/queue/notifications"), mock(MessageChannel.class));
        assertEquals(StompCommand.SUBSCRIBE, StompHeaderAccessor.wrap(result).getCommand());
    }

    @Test
    void subscribe_topic_allowed() {
        Message<?> result = interceptor.preSend(subscribeMessage("/topic/chat/123"), mock(MessageChannel.class));
        assertEquals(StompCommand.SUBSCRIBE, StompHeaderAccessor.wrap(result).getCommand());
    }

    @Test
    void subscribe_forbiddenDestination_rejected() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeMessage("/queue/admin-secret"), mock(MessageChannel.class)));
    }

    @Test
    void subscribe_withoutPrincipal_rejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/user/queue/notifications");
        accessor.setLeaveMutable(true);
        Message<byte[]> msg = org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, mock(MessageChannel.class)));
    }

    @Test
    void disconnect_passesThrough() {
        Message<?> result = interceptor.preSend(message(StompCommand.DISCONNECT), mock(MessageChannel.class));
        assertEquals(StompCommand.DISCONNECT, StompHeaderAccessor.wrap(result).getCommand());
        verify(jwtUtil, never()).validateAndGetUserId(anyString());
    }

    @Test
    void unsubscribe_passesThrough() {
        Message<?> result = interceptor.preSend(message(StompCommand.UNSUBSCRIBE), mock(MessageChannel.class));
        assertEquals(StompCommand.UNSUBSCRIBE, StompHeaderAccessor.wrap(result).getCommand());
        verify(jwtUtil, never()).validateAndGetUserId(anyString());
    }
}