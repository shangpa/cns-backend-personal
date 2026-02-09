package com.example.springjwt.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessages(String roomKey) {
        return chatMessageRepository.findByRoomKey(roomKey);
    }
}
