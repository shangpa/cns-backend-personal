package com.example.springjwt.mypage;

import com.example.springjwt.recipe.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyWriteRecipeDTO {
    private Long id;
    private String title;
    private String mainImageUrl;
    private String category;
    private int viewCount;
    private int likes;
    private String createdAt;

    public MyWriteRecipeDTO(Recipe recipe) {
        this.id = recipe.getRecipeId();
        this.title = recipe.getTitle();
        this.mainImageUrl = recipe.getMainImageUrl();
        this.category = recipe.getCategory().name();
        this.viewCount = recipe.getViewCount();
        this.likes = recipe.getLikes();
        this.createdAt = recipe.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static MyWriteRecipeDTO fromEntity(Recipe recipe) {
        return new MyWriteRecipeDTO(recipe); // 내부 생성자 사용
    }
}
