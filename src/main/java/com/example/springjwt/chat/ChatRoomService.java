package com.example.springjwt.chat;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.tradepost.TradePost;
import com.example.springjwt.tradepost.TradePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final TradePostRepository tradePostRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 채팅방 생성 또는 조회
    public ChatRoom createOrGetRoom(Long postId, Long senderId, Long ownerId) {
        String roomKey = makeRoomKey(senderId, ownerId, postId);

        return chatRoomRepository.findByRoomKey(roomKey)
                .orElseGet(() -> {
                    TradePost post = tradePostRepository.findById(postId).orElseThrow();
                    UserEntity sender = userRepository.findById(senderId.intValue()).orElseThrow();
                    UserEntity owner = userRepository.findById(ownerId.intValue()).orElseThrow();

                    ChatRoom newRoom = ChatRoom.builder()
                            .roomKey(roomKey)
                            .post(post)
                            .userA(sender)
                            .userB(owner)
                            .build();

                    return chatRoomRepository.save(newRoom);
                });
    }
    // 로그인한 사용자의 모든 채팅방을 조회하고, 마지막 메시지 포함 응답 DTO 생성
    public List<ChatRoomListResponseDTO> getChatRoomsForUser(UserEntity user) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserId(user.getId());
        List<ChatRoomListResponseDTO> result = new ArrayList<>();

        for (ChatRoom room : rooms) {
            // ✅ 상대방 식별
            UserEntity opponent = room.getUserA().getId() == user.getId()
                    ? room.getUserB() : room.getUserA();

            // ✅ 최근 메시지
            ChatMessage lastMsg = chatMessageRepository.findTopByRoomKeyOrderByCreatedAtDesc(room.getRoomKey());

            result.add(new ChatRoomListResponseDTO(
                    room.getRoomKey(),
                    opponent.getId(),
                    opponent.getUsername(),
                    lastMsg != null ? lastMsg.getMessage() : "",
                    lastMsg != null ? lastMsg.getCreatedAt() : null
            ));
        }

        return result;
    }

    // 사용자 ID 두 개로 고유 roomKey 생성
    private String makeRoomKey(Long a, Long b, Long postId) {
        return a + "-" + b + "-" + postId;
    }
}
