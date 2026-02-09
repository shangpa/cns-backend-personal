package com.example.springjwt.mypage;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecommendRecipeRepository extends JpaRepository<RecommendRecipe, Long> {
    Optional<RecommendRecipe> findByUserAndRecipe(UserEntity user, Recipe recipe);
    boolean existsByUserAndRecipe(UserEntity user, Recipe recipe);
    // 연도별
    @Query("SELECT MONTH(r.recommendedAt), COUNT(r) FROM RecommendRecipe r WHERE YEAR(r.recommendedAt) = :year GROUP BY MONTH(r.recommendedAt)")
    List<Object[]> countByYear(@Param("year") int year);

    // 월별
    @Query("SELECT DAY(r.recommendedAt), COUNT(r) FROM RecommendRecipe r WHERE YEAR(r.recommendedAt) = :year AND MONTH(r.recommendedAt) = :month GROUP BY DAY(r.recommendedAt)")
    List<Object[]> countByMonth(@Param("year") int year, @Param("month") int month);

    // 기간별
    @Query("SELECT DATE(r.recommendedAt), COUNT(r) FROM RecommendRecipe r WHERE r.recommendedAt BETWEEN :start AND :end GROUP BY DATE(r.recommendedAt)")
    List<Object[]> countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteAllByRecipe(Recipe recipe);

}
