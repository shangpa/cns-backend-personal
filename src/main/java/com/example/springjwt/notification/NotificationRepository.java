package com.example.springjwt.notification;

import com.example.springjwt.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserOrderByCreatedAtDesc(UserEntity user);

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(int userId);

    List<NotificationEntity> findByUserAndReadTrue(UserEntity user);
}