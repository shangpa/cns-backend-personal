package com.example.springjwt.admin;

import com.example.springjwt.admin.dto.BoardMonthlyStatsDTO;
import com.example.springjwt.admin.dto.RecipeListAdminDTO;
import com.example.springjwt.admin.dto.RecipeDetailAdminDTO;
import com.example.springjwt.admin.dto.RecipeReviewAdminDTO;
import com.example.springjwt.board.BoardCommentRepository;
import com.example.springjwt.board.BoardRepository;
import com.example.springjwt.recipe.Recipe;
import com.example.springjwt.recipe.RecipeRepository;
import com.example.springjwt.recipe.RecipeSearchResponseDTO;
import com.example.springjwt.recipeingredient.RecipeIngredientDTO;
import com.example.springjwt.review.Recipe.ReviewRepository;
import com.example.springjwt.tradepost.TradePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRecipeService {

    private final RecipeRepository recipeRepository;
    private final TradePostRepository tradePostRepository;
    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final ReviewRepository reviewRepository;

    // 인기 레시피 상위 3개 조회 (관리자용)
    public List<RecipeSearchResponseDTO> getTop3Recipes() {
        List<Recipe> top3 = recipeRepository.findTop3ByIsPublicTrueOrderByViewCountDesc();

        return top3.stream()
                .map(recipe -> RecipeSearchResponseDTO.fromEntity(recipe, 0.0, 0, false))
                .collect(Collectors.toList());
    }

    public List<BoardMonthlyStatsDTO> countCommentMonthly(LocalDateTime startDate) {
        List<Object[]> results = boardCommentRepository.countCommentMonthly(startDate);
        return results.stream()
                .map(obj -> new BoardMonthlyStatsDTO((String) obj[0], (long) ((Long) obj[1]).intValue()))
                .collect(Collectors.toList());
    }

    public Page<RecipeListAdminDTO> getRecipeListForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return recipeRepository.findAllForAdmin(pageable);
    }

    public Page<RecipeListAdminDTO> searchRecipesByTitle(String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return recipeRepository.searchByTitleForAdmin(title, pageable);
    }

    public RecipeDetailAdminDTO getRecipeDetail(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피가 존재하지 않습니다."));

        List<RecipeReviewAdminDTO> reviews = reviewRepository.findAllByRecipe(recipe).stream()
                .map(r -> new RecipeReviewAdminDTO(
                        r.getReviewId(),
                        r.getUser().getUsername(),
                        r.getContent(),
                        r.getRating(),
                        r.getMediaUrls(),
                        r.getCreatedAt()
                ))
                .toList();

        return RecipeDetailAdminDTO.builder()
                .recipeId(recipe.getRecipeId())
                .username(recipe.getUser().getUsername())
                .title(recipe.getTitle())
                .category(recipe.getCategory())
                .ingredients(
                        recipe.getIngredients() != null
                                ? recipe.getIngredients().stream()
                                .map(ri -> new RecipeIngredientDTO(
                                        ri.getIngredient().getId(),
                                        ri.getIngredient().getNameKo(),
                                        ri.getQuantity()
                                ))
                                .toList()
                                : null
                )
                .alternativeIngredients(recipe.getAlternativeIngredients())
                .handlingMethods(recipe.getHandlingMethods())
                .cookingSteps(recipe.getCookingSteps())
                .mainImageUrl(recipe.getMainImageUrl())
                .difficulty(recipe.getDifficulty())
                .tags(recipe.getTags())
                .cookingTime(recipe.getCookingTime())
                .servings(recipe.getServings())
                .createdAt(recipe.getCreatedAt())
                .isPublic(recipe.isPublic())
                .viewCount(recipe.getViewCount())
                .likes(recipe.getLikes())
                .recommends(recipe.getRecommends())
                .videoUrl(recipe.getVideoUrl())
                .reviews(reviews)
                .build();
    }
}
