package com.example.cns.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // roomKey 기준으로 이전 메시지 리스트 조회
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessagesByRoomKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("roomId") String roomKey
    ) {
        List<ChatMessage> messages = chatMessageService.getMessages(roomKey);
        return ResponseEntity.ok(messages);
    }
}