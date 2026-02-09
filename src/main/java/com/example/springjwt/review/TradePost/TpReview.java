package com.example.springjwt.review.TradePost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.tradepost.TradePost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tp_review")
public class TpReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tpReviewId; // 거래글 리뷰 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_post_id", nullable = false)
    private TradePost tradePost; // FK - 거래글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user; // FK - 리뷰 작성자

    @Column(nullable = false, length = 1000)
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private int rating; // 별점 (1~5)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 작성일시

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // 저장 시 자동 생성
    }
}