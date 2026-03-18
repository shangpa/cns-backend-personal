package com.example.cns.notification;

import com.example.cns.User.UserEntity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    public void sendNotification(String targetToken, String title, String body) {
        if (firebaseMessaging == null) {
            log.debug("FCM 비활성화 상태 — 알림 전송 생략");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            firebaseMessaging.send(message);
            log.debug("FCM 전송 성공");

        } catch (FirebaseMessagingException e) {
            log.warn("FCM 단건 전송 실패: {}", e.getMessage());
        }
    }
    public void sendNotificationToUser(UserEntity user, String title, String content, String category) {
        if (firebaseMessaging == null) {
            log.debug("FCM 비활성화 상태 — 알림 전송 생략");
            return;
        }
        // 1. 알림 DB 저장
        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setCategory(category);
        notification.setContent(content);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // 2. FCM 발송
        List<DeviceToken> tokens = deviceTokenRepository.findByUser(user);
        for (DeviceToken token : tokens) {
            Message message = Message.builder()
                    .setToken(token.getFcmToken())
                    .putData("title", title)
                    .putData("body", content)
                    .putData("category", category)
                    .build();

            try {
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM 전송 실패: {}", e.toString());
            }
        }
    }

    //채팅용
    public void sendChatNotification(UserEntity user, String content, String roomKey) {
        if (firebaseMessaging == null) {
            log.debug("FCM 비활성화 상태 — 채팅 알림 전송 생략");
            return;
        }
        List<DeviceToken> tokens = deviceTokenRepository.findByUser(user);
        for (DeviceToken token : tokens) {
            Message message = Message.builder()
                    .setToken(token.getFcmToken())
                    .putData("title", "새 채팅 메시지")
                    .putData("body", content)
                    .putData("category", "CHAT")
                    .putData("roomKey", roomKey)
                    .build();
            try {
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM 전송 실패: {}", e.toString());
                log.warn("실패한 토큰: {}", token.getFcmToken());
                log.warn("사용자 ID: {}", user.getId());
                if (e.getMessage().contains("Requested entity was not found")) {
                    deviceTokenRepository.delete(token);
                    log.warn("무효한 FCM 토큰 삭제됨: {}", token.getFcmToken());
                }
            }
        }
    }

    @Transactional
    public void deleteFcmToken(UserEntity user, String token) {
        deviceTokenRepository.deleteByUserAndFcmToken(user, token);
    }

}