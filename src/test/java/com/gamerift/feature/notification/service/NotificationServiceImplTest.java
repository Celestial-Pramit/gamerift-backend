package com.gamerift.feature.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.feature.notification.dto.NotificationResponse;
import com.gamerift.feature.notification.model.AppNotification;
import com.gamerift.feature.notification.repository.NotificationRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the notification rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationPusher notificationPusher;
    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private NotificationServiceImpl service;

    private AppNotification notification(String id, String userId, boolean read) {
        AppNotification n = new AppNotification();
        n.setId(id);
        n.setUserId(userId);
        n.setRead(read);
        return n;
    }

    private User user(String id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void getNotifications_mapsTheListNewestFirst() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(notification("n2", "u1", false), notification("n1", "u1", true)));

        List<NotificationResponse> result = service.getNotifications("u1");

        assertEquals(2, result.size());
        assertEquals("n2", result.get(0).getId());
        assertEquals("n1", result.get(1).getId());
    }

    @Test
    void setRead_marksTheNotificationAndSaves() {
        AppNotification n = notification("n1", "u1", false);
        when(notificationRepository.findByIdAndUserId("n1", "u1")).thenReturn(Optional.of(n));
        when(notificationRepository.save(n)).thenReturn(n);

        NotificationResponse response = service.setRead("u1", "n1", true);

        assertEquals(true, response.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllRead_onlySavesTheUnreadOnes() {
        AppNotification unread = notification("n1", "u1", false);
        AppNotification read = notification("n2", "u1", true);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(unread, read));

        service.markAllRead("u1");

        verify(notificationRepository).save(unread);
        verify(notificationRepository, never()).save(read);
    }

    @Test
    void delete_removesYourOwnNotification() {
        when(notificationRepository.findByIdAndUserId("n1", "u1")).thenReturn(Optional.of(notification("n1", "u1", false)));

        service.delete("u1", "n1");

        verify(notificationRepository).deleteById("n1");
    }

    @Test
    void delete_someoneElsesNotification_throwsNotFound() {
        when(notificationRepository.findByIdAndUserId("n1", "u1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("u1", "n1"));
        verify(notificationRepository, never()).deleteById(any());
    }

    @Test
    void send_followAlwaysArrivesButSwitchableTypesRespectTheSwitch() {
        User follower = user("u1", "alice");
        User viewer = user("u2", "bob");
        viewer.setNotifyLive(false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(follower));
        when(userRepository.findById("u2")).thenReturn(Optional.of(viewer));
        when(notificationRepository.save(any(AppNotification.class)))
                .thenAnswer(inv -> {
                    AppNotification n = inv.getArgument(0);
                    n.setId("saved-" + n.getType());
                    return n;
                });

        service.send("u1", "follow", "alice started following you", "/app/channel/x");
        service.send("u2", "live", "A stream went live", "/app/stream/s1");

        // follow has no switch: it always arrives
        verify(notificationRepository).save(argThat(n -> "follow".equals(n.getType())));
        // live respects the switch: bob turned it off
        verify(notificationRepository, never()).save(argThat(n -> "live".equals(n.getType())));
    }

    @Test
    void send_savesThenPushesWithCorrectUserId() {
        User u = user("u1", "alice");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(notificationRepository.save(any(AppNotification.class)))
                .thenAnswer(inv -> {
                    AppNotification n = inv.getArgument(0);
                    n.setId("n1");
                    return n;
                });

        service.send("u1", "pay", "Pro activated", "/app/settings");

        verify(notificationRepository).save(argThat(n -> "pay".equals(n.getType()) && "u1".equals(n.getUserId())));
        verify(notificationPusher).push(eq("u1"), argThat(dto -> "pay".equals(dto.getType())));
    }

    @Test
    void send_preferenceOff_doesNotSaveAndDoesNotPush() {
        User u = user("u2", "bob");
        u.setNotifyLive(false);
        when(userRepository.findById("u2")).thenReturn(Optional.of(u));

        service.send("u2", "live", "Stream live", "/app/stream/s1");

        verify(notificationRepository, never()).save(any());
        verify(notificationPusher, never()).push(anyString(), any());
    }

    @Test
    void send_pusherThrows_notificationStillSavedAndNoException() {
        // Use real pusher with mocked messaging template to test exception handling
        NotificationRepository realRepo = notificationRepository;
        UserRepository realUserRepo = userRepository;
        FollowRepository realFollowRepo = followRepository;
        org.springframework.messaging.simp.SimpMessagingTemplate mockTemplate = 
            org.mockito.Mockito.mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        doThrow(new RuntimeException("WebSocket down")).when(mockTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());
        NotificationPusher realPusher = new NotificationPusher(mockTemplate);
        NotificationServiceImpl realService = new NotificationServiceImpl(realRepo, realUserRepo, realPusher, realFollowRepo);

        User u = user("u1", "alice");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        AppNotification saved = new AppNotification();
        saved.setId("n1");
        saved.setUserId("u1");
        saved.setType("pay");
        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);

        realService.send("u1", "pay", "Pro activated", "/app/settings");

        verify(notificationRepository).save(argThat(n -> "pay".equals(n.getType())));
        verify(mockTemplate).convertAndSendToUser(eq("u1"), eq("/queue/notifications"), any());
    }

    @Test
    void broadcast_pushesOncePerRecipient() {
        User u1 = user("u1", "alice");
        User u2 = user("u2", "bob");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(notificationRepository.save(any(AppNotification.class)))
                .thenAnswer(inv -> {
                    AppNotification n = inv.getArgument(0);
                    n.setId("n-" + n.getUserId());
                    return n;
                });

        service.notifyEveryone("system", "Maintenance", "/app/notifications");

        verify(notificationPusher).push(eq("u1"), argThat(dto -> "system".equals(dto.getType())));
        verify(notificationPusher).push(eq("u2"), argThat(dto -> "system".equals(dto.getType())));
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(AppNotification.class));
    }
}