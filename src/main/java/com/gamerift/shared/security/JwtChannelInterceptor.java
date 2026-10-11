package com.gamerift.shared.security;

import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.MessageDeliveryException;


import java.security.Principal;

/**
 * Intercepts incoming STOMP frames and validates JWT on CONNECT.
 * For other commands (except DISCONNECT, HEARTBEAT, UNSUBSCRIBE), ensures a principal exists.
 * SUBSCRIBE destinations must start with /user/queue/ or /topic/.
 * SEND destinations must start with /app/.
 */
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtChannelInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtChannelInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        // CONNECT: authenticate via JWT in Authorization header
        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT rejected: missing or invalid Authorization header");
                throw new MessageDeliveryException("Unauthorized");
            }

            String token = authHeader.substring(7).trim();
            String userId = jwtUtil.validateAndGetUserId(token);
            if (userId == null) {
                log.warn("STOMP CONNECT rejected: invalid or expired token");
                throw new MessageDeliveryException("Unauthorized");
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.isBanned()) {
                log.warn("STOMP CONNECT rejected: user not found or banned: {}", userId);
                throw new MessageDeliveryException("Unauthorized");
            }

            // Create a Principal whose getName() returns the userId
            Principal principal = new StompPrincipal(userId);

            accessor.setUser(principal);
            // Persist the principal in the message headers so downstream handlers can access it
            accessor.setLeaveMutable(true);
            return org.springframework.messaging.support.MessageBuilder.fromMessage(message)
                    .setHeaders(accessor)
                    .build();
        }

        // DISCONNECT, UNSUBSCRIBE pass through without auth check
        if (StompCommand.DISCONNECT.equals(command)
                || StompCommand.UNSUBSCRIBE.equals(command)) {
            return message;
        }

        // For SUBSCRIBE, SEND, etc.: require an authenticated principal
        if (accessor.getUser() == null) {
            log.warn("STOMP {} rejected: no principal", command);
            throw new MessageDeliveryException("Unauthorized");
        }

        // SUBSCRIBE: only allow /user/queue/ and /topic/
        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || (!destination.startsWith("/user/queue/") && !destination.startsWith("/topic/"))) {
                log.warn("STOMP SUBSCRIBE rejected: forbidden destination {}", destination);
                throw new MessageDeliveryException("Forbidden destination");
            }
        }

        // SEND: only allow /app/ for now (chat comes later)
        if (StompCommand.SEND.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/app/")) {
                log.warn("STOMP SEND rejected: forbidden destination {}", destination);
                throw new MessageDeliveryException("Forbidden destination");
            }
        }

        return message;
    }

    /**
     * Simple Principal implementation for STOMP sessions.
     * getName() returns the userId string.
     */
    private static class StompPrincipal implements Principal {
        private final String userId;

        StompPrincipal(String userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return userId;
        }
    }
}