package com.example.cns.chat;

import com.example.cns.User.UserEntity;
import com.example.cns.notification.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final FCMService fcmService;
    private final ChatRoomRepository chatRoomRepository;

    @MessageMapping("/chat.send") // 클라이언트에서 "/app/chat.send" 로 보냄
    public void sendMessage(ChatMessage message) {
        System.out.println("📩 메시지 수신됨: " + message.getRoomKey() + " / " + message.getMessage());
        chatMessageService.save(message); // DB 저장
        messagingTemplate.convertAndSend(
                "/topic/chatroom/" + message.getRoomKey(),
                message);
        sendChatNotification(message);
    }
    private void sendChatNotification(ChatMessage message) {
        ChatRoom room = chatRoomRepository.findByRoomKey(message.getRoomKey()).orElseThrow();

        Long senderId = message.getSenderId();
        UserEntity receiver = room.getUserA().getId() == senderId
                ? room.getUserB()
                : room.getUserA();

        // 채팅 전용 FCM 알림 전송 (roomKey 포함)
        fcmService.sendChatNotification(
                receiver, message.getMessage(), message.getRoomKey()
        );
    }
}
