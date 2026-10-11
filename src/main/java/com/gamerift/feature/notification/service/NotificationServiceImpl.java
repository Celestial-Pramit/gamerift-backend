package com.gamerift.feature.notification.service;

import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.feature.notification.dto.NotificationResponse;
import com.gamerift.feature.notification.model.AppNotification;
import com.gamerift.feature.notification.repository.NotificationRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for notifications (layer: service).
 *  - every command is scoped to the logged-in user (a notification belongs to exactly one user),
 *  - send() checks the user's alert switches: replies/pay/live/event respect the preference,
 *    follow and system always arrive,
 *  - send() is called by the other features (replies, payments, bans, broadcast).
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPusher notificationPusher;
    private final FollowRepository followRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository,
                                   NotificationPusher notificationPusher, FollowRepository followRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationPusher = notificationPusher;
        this.followRepository = followRepository;
    }

    @Override
    public List<NotificationResponse> getNotifications(String userId) {
        List<NotificationResponse> result = new ArrayList<>();
        for (AppNotification n : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            result.add(NotificationMapper.toResponse(n));
        }
        return result;
    }

    @Override
    public NotificationResponse setRead(String userId, String notificationId, boolean read) {
        AppNotification n = findOwned(userId, notificationId);
        n.setRead(read);
        return NotificationMapper.toResponse(notificationRepository.save(n));
    }

    @Override
    public void markAllRead(String userId) {
        for (AppNotification n : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }

    @Override
    public void delete(String userId, String notificationId) {
        findOwned(userId, notificationId);
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void send(String userId, String type, String title, String targetUrl) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !wants(type, user)) {
            return;
        }
        AppNotification n = new AppNotification();
        n.setUserId(userId);
        n.setType(type.toLowerCase());
        n.setTitle(title);
        n.setTargetUrl(targetUrl);
        AppNotification saved = notificationRepository.save(n);
        NotificationResponse dto = NotificationMapper.toResponse(saved);
        notificationPusher.push(userId, dto);
    }

    @Override
    public void notifyUsers(List<String> userIds, String type, String title, String targetUrl) {
        List<User> users = new ArrayList<>();
        for (String userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                users.add(user);
            }
        }
        store(users, type, title, targetUrl);
    }

    @Override
    public void notifyEveryone(String type, String title, String targetUrl) {
        store(userRepository.findAll(), type, title, targetUrl);
    }

    @Override
    public void notifyChannelLive(String channelId, String channelName, String streamTitle, String targetUrl) {
        List<String> followerIds = followRepository.findUserIdsByChannelId(channelId);
        // Cap at 500 followers per call
        if (followerIds.size() > 500) {
            followerIds = followerIds.subList(0, 500);
        }
        String title = channelName + " is live: " + streamTitle;
        notifyUsers(followerIds, "live", title, targetUrl);
    }

    @Override
    public void notifyEventCreated(String eventId, String title, String channelId) {
        List<String> userIds = new ArrayList<>();
        if (channelId != null && !channelId.isBlank()) {
            // Event belongs to a channel: notify its followers
            userIds = followRepository.findUserIdsByChannelId(channelId);
        } else {
            // No channel: notify users with notifyEvents = true
            List<User> users = userRepository.findByNotifyEventsTrue();
            for (User user : users) {
                userIds.add(user.getId());
            }
        }
        // Cap at 500 users per call
        if (userIds.size() > 500) {
            userIds = userIds.subList(0, 500);
        }
        String notificationTitle = "New event: " + title;
        String targetUrl = "/app/events";
        notifyUsers(userIds, "event", notificationTitle, targetUrl);
    }

    // one notification per user, filtered by the alert switches
    private void store(List<User> users, String type, String title, String targetUrl) {
        String clean = type == null ? "" : type.trim().toLowerCase();
        for (User user : users) {
            if (!wants(clean, user)) {
                continue;
            }
            AppNotification n = new AppNotification();
            n.setUserId(user.getId());
            n.setType(clean);
            n.setTitle(title);
            n.setTargetUrl(targetUrl);
            AppNotification saved = notificationRepository.save(n);
            NotificationResponse dto = NotificationMapper.toResponse(saved);
            notificationPusher.push(user.getId(), dto);
        }
    }

    private boolean wants(String type, User user) {
        return switch (type == null ? "" : type.toLowerCase()) {
            case "live" -> user.isNotifyLive();
            case "event" -> user.isNotifyEvents();
            case "reply" -> user.isNotifyReplies();
            case "pay" -> user.isNotifyPay();
            // follow and system always arrive
            default -> true;
        };
    }

    private AppNotification findOwned(String userId, String notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }
}