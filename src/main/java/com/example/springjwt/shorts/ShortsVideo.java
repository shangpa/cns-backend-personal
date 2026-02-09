package com.example.springjwt.shorts;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.shorts.comment.ShortComment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shorts_video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortsVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;        // 영상 제목
    private String videoUrl;     // 업로드된 영상 경로
    private String thumbnailUrl; // 썸네일 (선택)

    @Column(name = "is_public", nullable = false)   // ✅ 명시
    private boolean isPublic = true;

    private int viewCount = 0;   // 조회수
    private int likeCount = 0;       // 좋아요 수

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;     // 업로드한 유저


    @OneToMany(mappedBy = "shortsVideo")
    private List<ShortComment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int commentCount = 0;
}
