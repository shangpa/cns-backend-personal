package com.example.springjwt.chat;

import com.example.springjwt.tradepost.TradePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomKey(String roomKey);

    // 로그인한 사용자가 userA 또는 userB인 채팅방 모두 조회
    @Query("SELECT c FROM ChatRoom c WHERE c.userA.id = :userId OR c.userB.id = :userId")
    List<ChatRoom> findByUserId(@Param("userId") int userId);

    @Query("SELECT COUNT(c) FROM ChatRoom c WHERE c.post.tradePostId = :postId")
    int countByTradePostId(@Param("postId") Long postId);

    List<ChatRoom> findAllByPost(TradePost post);
}
