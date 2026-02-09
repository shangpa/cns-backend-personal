package com.example.springjwt.review.Recipe;


import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRecipe_RecipeId(Long recipeId); // 특정 레시피의 리뷰 조회
    //평균점수
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.recipe.recipeId = :recipeId")
    Double findAvgRatingByRecipe(@Param("recipeId") Long recipeId);
    //리뷰수
    int countByRecipe(Recipe recipe);
    //마이페이지 - 리뷰내역
    List<Review> findByUser(UserEntity user);
    //마이페이지 - 리뷰삭제
    List<Review> findByUserAndRecipe_Category(UserEntity user, com.example.springjwt.recipe.RecipeCategory category);
    int countByUser(UserEntity user);
    List<Review> findAllByRecipe(Recipe recipe);
    void deleteAllByRecipe(Recipe recipe);
}