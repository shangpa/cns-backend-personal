package com.example.springjwt.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponseDTO {
    private String roomKey;
    private Long postId;
    private int userAId;
    private int userBId;

    public static ChatRoomResponseDTO from(ChatRoom room) {
        return new ChatRoomResponseDTO(
                room.getRoomKey(),
                room.getPost().getTradePostId(),
                room.getUserA().getId(),
                room.getUserB().getId()
        );
    }
}