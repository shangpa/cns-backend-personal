package com.example.cns.review.Recipe;
import com.example.cns.User.UserEntity;
import com.example.cns.recipe.Recipe;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequestDTO {
    private Long recipeId;
    private String content;
    private int rating;
    private String mediaUrls;
    public Review toEntity(UserEntity user, Recipe recipe) {
        return Review.builder()
                .user(user)
                .recipe(recipe)
                .content(content)
                .rating(rating)
                .mediaUrls(mediaUrls)
                .build();
    }
}