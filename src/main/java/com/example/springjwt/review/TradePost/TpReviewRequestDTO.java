package com.example.springjwt.review.TradePost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.tradepost.TradePost;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TpReviewRequestDTO {

    private Long tradePostId; // 거래글 ID
    private String content;   // 리뷰 내용
    private int rating;       // 별점

    public TpReview toEntity(UserEntity user, TradePost tradePost) {
        return TpReview.builder()
                .user(user)
                .tradePost(tradePost)
                .content(content)
                .rating(rating)
                .build();
    }
}