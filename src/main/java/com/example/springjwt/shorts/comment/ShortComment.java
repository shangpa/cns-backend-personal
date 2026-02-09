package com.example.springjwt.shorts.comment;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.board.Board;
import com.example.springjwt.shorts.ShortsVideo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ShortComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private ShortsVideo shortsVideo;

    private LocalDateTime createdAt = LocalDateTime.now();
}
