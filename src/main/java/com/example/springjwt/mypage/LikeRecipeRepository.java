package com.example.springjwt.mypage;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LikeRecipeRepository extends JpaRepository<LikeRecipe, Long> {
    boolean existsByUserAndRecipe(UserEntity user, Recipe recipe);
    Optional<LikeRecipe> findByUserAndRecipe(UserEntity user, Recipe recipe);
    List<LikeRecipe> findByUser(UserEntity user);
    int countByRecipe(Recipe recipe);

    void delete(LikeRecipe likeRecipe);

    // 연도별
    @Query("SELECT MONTH(l.likedAt), COUNT(l) FROM LikeRecipe l WHERE YEAR(l.likedAt) = :year GROUP BY MONTH(l.likedAt)")
    List<Object[]> countLikesByYear(@Param("year") int year);

    // 월별
    @Query("SELECT DAY(l.likedAt), COUNT(l) FROM LikeRecipe l WHERE YEAR(l.likedAt) = :year AND MONTH(l.likedAt) = :month GROUP BY DAY(l.likedAt)")
    List<Object[]> countLikesByMonth(@Param("year") int year, @Param("month") int month);

    // 기간별
    @Query("SELECT DATE(l.likedAt), COUNT(l) FROM LikeRecipe l WHERE l.likedAt BETWEEN :start AND :end GROUP BY DATE(l.likedAt)")
    List<Object[]> countLikesByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    void deleteAllByRecipe(Recipe recipe);
}
