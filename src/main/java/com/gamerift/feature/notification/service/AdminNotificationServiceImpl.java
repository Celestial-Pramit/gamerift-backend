package com.gamerift.feature.notification.service;

import com.gamerift.feature.notification.dto.NotificationRequest;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.springframework.stereotype.Service;

/** Business rule: a broadcast becomes one notification per user, going through send() so the alerts are honoured. */
@Service
public class AdminNotificationServiceImpl implements IAdminNotificationService {

    private final UserRepository userRepository;
    private final INotificationService notificationService;

    public AdminNotificationServiceImpl(UserRepository userRepository, INotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void broadcast(NotificationRequest request) {
        String type = request.getType().trim().toLowerCase();
        String title = request.getTitle().trim();
        for (User user : userRepository.findAll()) {
            notificationService.send(user.getId(), type, title, "/app/notifications");
        }
    }
}