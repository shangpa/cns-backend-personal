package com.example.springjwt.board;

import com.example.springjwt.User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity writer;

    private String content;

    @ElementCollection
    private List<String> imageUrls;  // 이미지 URL 여러 개

    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    private int likeCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "board")
    private List<BoardComment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int commentCount = 0;
}
