package com.example.springjwt.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-message")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // roomKey 기준으로 이전 메시지 리스트 조회
    @GetMapping
    public ResponseEntity<List<ChatMessage>> getMessagesByRoomKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String roomKey
    ) {
        List<ChatMessage> messages = chatMessageService.getMessages(roomKey);
        return ResponseEntity.ok(messages);
    }
}