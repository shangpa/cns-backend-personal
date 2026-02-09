package com.example.springjwt.chat;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.tradepost.TradePost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_key", unique = true)
    private String roomKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private TradePost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id")
    private UserEntity userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id")
    private UserEntity userB;

    private LocalDateTime updatedAt;
    @PrePersist
    public void setUpdatedAt() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

}
