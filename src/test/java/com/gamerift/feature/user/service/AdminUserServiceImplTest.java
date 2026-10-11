package com.gamerift.feature.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.model.Follow;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.notification.repository.NotificationRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.user.dto.AdminUserResponse;
import com.gamerift.feature.user.dto.AdminUserUpdateRequest;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin user rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private INotificationService notificationService;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EventReminderRepository reminderRepository;

    @InjectMocks
    private AdminUserServiceImpl service;

    private User user(String id, String username, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    private AdminUserUpdateRequest request(Role role, boolean banned) {
        AdminUserUpdateRequest r = new AdminUserUpdateRequest();
        r.setRole(role);
        r.setBanned(banned);
        return r;
    }

    private AdminUserUpdateRequest banOnly(boolean banned) {
        AdminUserUpdateRequest r = new AdminUserUpdateRequest();
        r.setBanned(banned);
        return r;
    }

    @Test
    void getAll_sortedByUsername() {
        when(userRepository.findAll()).thenReturn(List.of(user("2", "zz", Role.VIEWER), user("1", "aa", Role.VIEWER)));

        List<AdminUserResponse> result = service.getAll();

        assertEquals("aa", result.get(0).getUsername());
        assertEquals("zz", result.get(1).getUsername());
    }

    @Test
    void update_banSendsNotification() {
        User viewer = user("u2", "bob", Role.VIEWER);
        when(userRepository.findById("u2")).thenReturn(Optional.of(viewer));
        when(userRepository.save(viewer)).thenReturn(viewer);

        AdminUserResponse response = service.update("u1", "u2", banOnly(true));

        assertTrue(response.isBanned());
        verify(notificationService).send("u2", "system", "You have been banned from Gamerift", "/app/notifications");
    }

    @Test
    void update_ownRoleChange_throwsForbidden() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "admin", Role.ADMIN)));

        assertThrows(ForbiddenException.class, () -> service.update("u1", "u1", request(Role.VIEWER, false)));
    }

    @Test
    void update_anotherAdmin_throwsForbidden() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "other", Role.ADMIN)));

        assertThrows(ForbiddenException.class, () -> service.update("u1", "u2", request(Role.VIEWER, false)));
    }

    private User deleteTestUser(String id, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername("name_" + id);
        u.setRole(role);
        return u;
    }

    @Test
    void delete_viewer_removesFollowsAndTakesOneFollowerFromEachChannel() {
        Follow follow = new Follow();
        follow.setUserId("u1");
        follow.setChannelId("c1");
        Channel channel = new Channel();
        channel.setId("c1");
        channel.setFollowersCount(5);
        when(userRepository.findById("u1")).thenReturn(Optional.of(deleteTestUser("u1", Role.VIEWER)));
        when(followRepository.findByUserId("u1")).thenReturn(List.of(follow));
        when(channelRepository.findAllById(List.of("c1"))).thenReturn(List.of(channel));

        service.delete("admin1", "u1");

        assertEquals(4, channel.getFollowersCount());
        verify(followRepository).deleteAll(List.of(follow));
        verify(notificationRepository).deleteByUserId("u1");
        verify(reminderRepository).deleteByUserId("u1");
        verify(userRepository).deleteById("u1");
    }

    @Test
    void delete_ownAccount_throwsForbidden() {
        when(userRepository.findById("admin1")).thenReturn(Optional.of(deleteTestUser("admin1", Role.VIEWER)));

        assertThrows(ForbiddenException.class, () -> service.delete("admin1", "admin1"));
    }

    @Test
    void delete_anotherAdmin_throwsForbiddenAndDeletesNothing() {
        when(userRepository.findById("admin2")).thenReturn(Optional.of(deleteTestUser("admin2", Role.ADMIN)));

        assertThrows(ForbiddenException.class, () -> service.delete("admin1", "admin2"));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void delete_unknownUser_throwsNotFound() {
        when(userRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("admin1", "zzz"));
    }

    private Follow follow(String id, String userId, String channelId) {
        Follow f = new Follow();
        f.setId(id);
        f.setUserId(userId);
        f.setChannelId(channelId);
        return f;
    }

    private Channel channel(String id, String name) {
        Channel c = new Channel();
        c.setId(id);
        c.setName(name);
        return c;
    }
}