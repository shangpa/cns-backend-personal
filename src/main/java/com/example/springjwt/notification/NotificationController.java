package com.example.springjwt.notification;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FCMService fcmService;

    //알림 목록 불러오기
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @RequestHeader("Authorization") String token) {

        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);

        List<NotificationEntity> notifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user);

        // DTO 변환
        List<NotificationResponseDTO> dtoList = notifications.stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtoList);
    }
    //알림 기능삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id,
                                                   @RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("알림이 존재하지 않습니다"));

        // 보안: 본인 알림만 삭제 가능
        if (notification.getUser().getId() != user.getId()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        notificationRepository.delete(notification);
        return ResponseEntity.ok().build();
    }

    //읽은알림삭제
    @DeleteMapping("/read/all")
    public ResponseEntity<Void> deleteAllReadNotifications(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);

        List<NotificationEntity> readNotifications = notificationRepository.findByUserAndReadTrue(user);
        notificationRepository.deleteAll(readNotifications);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Void> testSendNotification(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        UserEntity user = userRepository.findByUsername(username);
        List<DeviceToken> tokens = deviceTokenRepository.findByUser(user);

        for (DeviceToken deviceToken : tokens) {
            fcmService.sendNotification(deviceToken.getFcmToken(),
                    "테스트 알림",
                    "이 알림이 보이면 성공!");
        }

        return ResponseEntity.ok().build();
    }
}
