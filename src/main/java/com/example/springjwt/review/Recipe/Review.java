package com.example.springjwt.review.Recipe;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipe.Recipe;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId; // 리뷰 PK

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe; // FK - 리뷰가 연결된 레시피

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user; // FK - 리뷰 작성자

    @Column(nullable = false, length = 1000)
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private int rating; // 별점 (1~5)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String mediaUrls; // 이미지 URL 리스트 (JSON 형식 문자열)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 작성일시

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // 리뷰 작성 시간 자동 설정
    }
}