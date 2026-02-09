package com.example.springjwt.review.Recipe;


import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.User.UserService;
import com.example.springjwt.admin.log.AdminLogService;
import com.example.springjwt.notification.FCMService;
import com.example.springjwt.recipe.Recipe;
import com.example.springjwt.recipe.RecipeCategory;
import com.example.springjwt.recipe.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;
    private final AdminLogService adminLogService;

    // 리뷰 작성
    @Transactional
    public ReviewResponseDTO createReview(ReviewRequestDTO dto) {
        // 현재 로그인한 사용자 정보 가져오기
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        // 유저 엔티티 찾기
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 레시피 엔티티 찾기
        Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다."));

        Review review = dto.toEntity(user, recipe);
        UserEntity recipeOwner = recipe.getUser();
        if (!recipeOwner.getUsername().equals(username)) { // 자기 자신에게는 알림 X
            fcmService.sendNotificationToUser(
                    recipeOwner,
                    "리뷰 알림",
                    user.getUsername() + "님이 당신의 레시피에 리뷰를 남겼습니다.",
                    "RECIPE"
            );
        }
        reviewRepository.save(review);
        return ReviewResponseDTO.fromEntity(review);
    }

    // 특정 레시피의 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByRecipe(Long recipeId) {
        List<Review> reviews = reviewRepository.findByRecipe_RecipeId(recipeId);
        return reviews.stream()
                .map(ReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 로그인한 사용자가 작성한 리뷰 목록
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByUser() {
        // 현재 로그인한 사용자
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        // 사용자 엔티티 조회
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        List<Review> reviews = reviewRepository.findByUser(user);

        return reviews.stream()
                .map(ReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    //마이페이지 - 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId, UserEntity user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (review.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    //마이페이지 - 리뷰 카테고리
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByUserAndCategory(UserEntity user, String category) {
        List<Review> reviews;

        if (category == null || category.equals("all")) {
            reviews = reviewRepository.findByUser(user);
        } else {
            // RecipeCategory enum으로 변환
            com.example.springjwt.recipe.RecipeCategory catEnum = com.example.springjwt.recipe.RecipeCategory.valueOf(category);
            reviews = reviewRepository.findByUserAndRecipe_Category(user, catEnum);
        }

        return reviews.stream()
                .map(ReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
    @Transactional
    public void deleteReviewByAdmin(Long reviewId, String adminUsername, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        reviewRepository.delete(review);

        adminLogService.logAdminAction(
                adminUsername,
                "DELETE_RECIPE_REVIEW",
                "RECIPE_REVIEW",
                reviewId,
                reason
        );
    }

}