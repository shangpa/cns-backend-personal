package com.example.springjwt.review.Recipe;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.User.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(@RequestBody ReviewRequestDTO dto) {
        return ResponseEntity.ok(reviewService.createReview(dto));
    }

    // 특정 레시피의 리뷰 조회
    @GetMapping("/{recipeId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@PathVariable Long recipeId) {
        return ResponseEntity.ok(reviewService.getReviewsByRecipe(recipeId));
    }

    // 마이페이지 - 리뷰 내역
    // 로그인한 사용자가 작성한 리뷰 조회
    @GetMapping("/mypage")
    public ResponseEntity<List<ReviewResponseDTO>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getReviewsByUser());
    }

    //마이페이지 - 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        reviewService.deleteReview(reviewId, user);
        return ResponseEntity.ok().build();
    }

    //마이페이지 - 리뷰 카테고리
    @GetMapping("/mypage/filter")
    public ResponseEntity<List<ReviewResponseDTO>> getMyReviewsByCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "category", required = false) String category
    ) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ReviewResponseDTO> result = reviewService.getReviewsByUserAndCategory(user, category);
        return ResponseEntity.ok(result);
    }
}