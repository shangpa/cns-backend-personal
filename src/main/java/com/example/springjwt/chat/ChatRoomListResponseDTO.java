package com.example.springjwt.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 채팅방 리스트 응답 DTO
 * - 로그인한 유저 기준 상대방 이름
 * - 마지막 메시지 내용
 * - 마지막 메시지 시간 포함
 */
@Data
@AllArgsConstructor
public class ChatRoomListResponseDTO {
    private String roomKey;
    private int opponentId;
    private String opponentUsername;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
}