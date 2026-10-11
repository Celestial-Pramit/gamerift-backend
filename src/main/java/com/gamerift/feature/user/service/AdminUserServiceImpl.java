package com.gamerift.feature.user.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

import org.springframework.stereotype.Service;

/**
 * Business rules for the admin "Users" screen (layer: service).
 *  - an admin can never change their own role or ban switch,
 *  - admins are protected from other admins (role/ban changes and deletion are blocked),
 *  - banning someone tells them with a system notification,
 *  - deleting a user also deletes their notifications, event reminders and follows,
 *    plus any channel that carries their streamer name (and the follows of that channel).
 */
@Service
public class AdminUserServiceImpl implements IAdminUserService {

    private final UserRepository userRepository;
    private final INotificationService notificationService;
    private final FollowRepository followRepository;
    private final ChannelRepository channelRepository;
    private final NotificationRepository notificationRepository;
    private final EventReminderRepository reminderRepository;

    public AdminUserServiceImpl(UserRepository userRepository, INotificationService notificationService,
                                FollowRepository followRepository, ChannelRepository channelRepository,
                                NotificationRepository notificationRepository, EventReminderRepository reminderRepository) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.followRepository = followRepository;
        this.channelRepository = channelRepository;
        this.notificationRepository = notificationRepository;
        this.reminderRepository = reminderRepository;
    }

    @Override
    public List<AdminUserResponse> getAll() {
        List<User> users = new ArrayList<>(userRepository.findAll());
        users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        List<AdminUserResponse> result = new ArrayList<>();
        for (User user : users) {
            result.add(AdminUserMapper.toResponse(user));
        }
        return result;
    }

    @Override
    public AdminUserResponse update(String adminId, String userId, AdminUserUpdateRequest request) {
        // 1. the target must exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean touchingOwnPrivileges = user.getId().equals(adminId)
                && (request.getRole() != null || request.isBanned());
        boolean demotingOrBanningAdmin = user.getRole() == Role.ADMIN
                && (request.getRole() != null && request.getRole() != Role.ADMIN || request.isBanned());

        // 2. two hard rules
        if (touchingOwnPrivileges) {
            throw new ForbiddenException("You cannot change your own role or ban status");
        }
        if (demotingOrBanningAdmin) {
            throw new ForbiddenException("You cannot edit another admin");
        }

        // 3. apply (a null role keeps the current one)
        if (request.getRole() != null && request.getRole() != user.getRole()) {
            user.setRole(request.getRole());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry().trim());
        }
        if (request.isBanned() && !user.isBanned()) {
            user.setBanned(true);
            notificationService.send(userId, "system", "You have been banned from Gamerift", "/app/notifications");
        }

        // 4. save and answer with the new row
        return AdminUserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void delete(String adminId, String userId) {
        // 1. the target must exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        // 2. admins cannot delete themselves or another admin
        if (user.getId().equals(adminId)) {
            throw new ForbiddenException("You cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("You cannot delete another admin");
        }
        // 3. every follow of this user takes one follower away from its channel
        List<Follow> follows = followRepository.findByUserId(userId);
        List<String> channelIds = new ArrayList<>();
        for (Follow follow : follows) {
            channelIds.add(follow.getChannelId());
        }
        for (Channel channel : channelRepository.findAllById(channelIds)) {
            channel.setFollowersCount(Math.max(0, channel.getFollowersCount() - 1));
            channelRepository.save(channel);
        }
        followRepository.deleteAll(follows);
        // 4. the rest of the account data, then the account itself
        notificationRepository.deleteByUserId(userId);
        reminderRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}