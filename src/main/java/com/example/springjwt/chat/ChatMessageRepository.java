package com.example.springjwt.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomKey(String roomKey);

    // 특정 채팅방(roomKey)에서 가장 마지막 메시지 한 건 가져오기
    ChatMessage findTopByRoomKeyOrderByCreatedAtDesc(String roomKey);
    void deleteAllByRoomKey(String roomKey);
}