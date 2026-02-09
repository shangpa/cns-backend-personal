package com.example.springjwt.notification;

import com.example.springjwt.User.UserEntity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class FCMService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String targetToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ FCM 전송 성공");

        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
    public void sendNotificationToUser(UserEntity user, String title, String content, String category) {
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
                System.out.println("FCM 전송 실패"+e);
            }
        }
    }

    //채팅용
    public void sendChatNotification(UserEntity user, String content, String roomKey) {
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
                System.out.println("FCM 전송 실패"+e);
                System.out.println("실패한 토큰: " + token.getFcmToken());
                System.out.println("사용자 ID: " + user.getId());
                if (e.getMessage().contains("Requested entity was not found")) {
                    deviceTokenRepository.delete(token);
                    System.out.println("🗑️ 무효한 FCM 토큰 삭제됨: " + token.getFcmToken());
                }
            }
        }
    }

    @Transactional
    public void deleteFcmToken(UserEntity user, String token) {
        deviceTokenRepository.deleteByUserAndFcmToken(user, token);
    }

}