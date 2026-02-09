package com.example.springjwt.review.TradePost;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TpReviewResponseDTO {

    private Long tpReviewId;   // 리뷰 ID
    private String content;    // 리뷰 내용
    private int rating;        // 별점
    private LocalDateTime createdAt; // 작성일시
    private String username;   // 작성자 이름
    private String tradeTitle;
    public static TpReviewResponseDTO fromEntity(TpReview tpReview) {
        return TpReviewResponseDTO.builder()
                .tpReviewId(tpReview.getTpReviewId())
                .content(tpReview.getContent())
                .rating(tpReview.getRating()    )
                .createdAt(tpReview.getCreatedAt())
                .username(tpReview.getUser().getUsername())
                .tradeTitle(tpReview.getTradePost().getTitle())
                .build();
    }
}