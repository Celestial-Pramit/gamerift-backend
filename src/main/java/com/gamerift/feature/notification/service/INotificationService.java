package com.gamerift.feature.notification.service;

import java.util.List;

import com.gamerift.feature.notification.dto.NotificationResponse;

/** Read, mark and delete my notifications + create notifications from the other features. */
public interface INotificationService {

    List<NotificationResponse> getNotifications(String userId);

    NotificationResponse setRead(String userId, String notificationId, boolean read);

    void markAllRead(String userId);

    void delete(String userId, String notificationId);

    /**
     * Creates a notification when the user still wants this type of alert
     * (live/event/reply/pay check the user's preference; follow/system always go through).
     */
    void send(String userId, String type, String title, String targetUrl);

    /** Creates one notification for each of the given users, honouring their alert switches. */
    void notifyUsers(List<String> userIds, String type, String title, String targetUrl);

    /** Creates one notification for every registered user, honouring their alert switches. */
    void notifyEveryone(String type, String title, String targetUrl);

    /**
     * Notifies followers of a channel that a stream is now live.
     * Caps at 500 followers per call. Respects each user's notifyLive preference via send().
     */
    void notifyChannelLive(String channelId, String channelName, String streamTitle, String targetUrl);

    /**
     * Notifies about a newly created event.
     * If channelId is provided, notifies that channel's followers (cap 500).
     * Otherwise notifies users with notifyEvents=true (cap 500).
     * Respects each user's notifyEvents preference via send().
     */
    void notifyEventCreated(String eventId, String title, String channelId);
}