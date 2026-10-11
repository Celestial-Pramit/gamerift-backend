package com.gamerift.feature.notification.service;

import com.gamerift.feature.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPusherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationPusher pusher;

    @Test
    void push_callsConvertAndSendToUserWithCorrectArguments() {
        NotificationResponse dto = new NotificationResponse();
        dto.setId("n1");
        dto.setType("pay");
        dto.setTitle("Test");

        pusher.push("u1", dto);

        verify(messagingTemplate).convertAndSendToUser("u1", "/queue/notifications", dto);
    }

    @Test
    void push_doesNotThrowWhenTemplateThrows() {
        NotificationResponse dto = new NotificationResponse();
        doThrow(new RuntimeException("WebSocket error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        pusher.push("u1", dto); // should not throw

        verify(messagingTemplate).convertAndSendToUser("u1", "/queue/notifications", dto);
    }
}