package com.example.springjwt.notification;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    //알림 전송
    public void notifyUser(NotificationRequestDTO dto) {
        UserEntity user = userRepository.findById(dto.getUserId()).orElseThrow();

        // 알림 저장
        NotificationEntity entity = new NotificationEntity();
        entity.setUser(user);
        entity.setCategory(dto.getCategory());
        entity.setContent(dto.getContent());
        entity.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(entity);

        // 모든 기기 토큰에 FCM 발송
        List<DeviceToken> tokens = tokenRepository.findByUser(user);
        for (DeviceToken token : tokens) {
            sendFCM(token.getFcmToken(), dto.getCategory(), dto.getContent());
        }
    }

    private void sendFCM(String targetToken, String category, String content) {
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle("[" + category + "]")
                            .setBody(content)
                            .build())
                    .putData("category", category)
                    .putData("content", content)
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}
