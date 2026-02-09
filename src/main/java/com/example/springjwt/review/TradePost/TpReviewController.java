package com.example.springjwt.review.TradePost;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tp-reviews")
@RequiredArgsConstructor
public class TpReviewController {

    private final TpReviewService tpReviewService;

    // 거래글 리뷰 작성
    @PostMapping
    public ResponseEntity<TpReviewResponseDTO> createTpReview(@RequestBody TpReviewRequestDTO dto) {
        return ResponseEntity.ok(tpReviewService.createTpReview(dto));
    }

    // 특정 거래글의 리뷰 조회
    @GetMapping("/{tradePostId}")
    public ResponseEntity<List<TpReviewResponseDTO>> getTpReviews(@PathVariable Long tradePostId) {
        return ResponseEntity.ok(tpReviewService.getTpReviewsByTradePost(tradePostId));
    }
    // 내가 쓴 리뷰 리스트
    @GetMapping("/my-reviews")
    public ResponseEntity<List<TpReviewResponseDTO>> getMyTpReviews() {
        return ResponseEntity.ok(tpReviewService.getMyTpReviews());
    }

    // 내 거래글에 달린 리뷰 리스트
    @GetMapping("/reviews-on-my-posts")
    public ResponseEntity<List<TpReviewResponseDTO>> getReviewsOnMyTradePosts() {
        return ResponseEntity.ok(tpReviewService.getReviewsOnMyTradePosts());
    }

    // username으로 특정 유저의 거래글에 달린 리뷰 조회
    @GetMapping("/reviews-on-user/username/{username}")
    public ResponseEntity<List<TpReviewResponseDTO>> getReviewsOnUserTradePostsByUsername(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) { // 토큰을 받는다
        return ResponseEntity.ok(tpReviewService.getReviewsOnUserTradePostsByUsername(username));
    }
}