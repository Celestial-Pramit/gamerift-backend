package com.gamerift.feature.notification.repository;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.notification.model.AppNotification;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<AppNotification, String> {

    List<AppNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<AppNotification> findByIdAndUserId(String id, String userId);

    void deleteByUserId(String userId);
}