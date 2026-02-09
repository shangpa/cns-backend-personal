package com.example.cns.recipe;

import com.example.cns.User.UserEntity;
import com.example.cns.recipeingredient.RecipeIngredient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recipeId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;  // 레시피 작성자 (외래 키)

    private String title;

    @Enumerated(EnumType.STRING)
    private RecipeCategory category; // 카테고리 (ENUM)

    /* 재료 수정중
    @Lob
    @Column(columnDefinition = "TEXT")
    private String ingredients; // JSON 형식의 재료
    */
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    /** orphanRemoval=true에서 안전하게 자식 목록을 교체하는 헬퍼 */
    public void replaceIngredients(List<RecipeIngredient> newOnes) {
        this.ingredients.clear();                 // 프록시 유지 + 기존 orphan 삭제
        if (newOnes == null) return;
        for (RecipeIngredient ri : newOnes) {
            ri.setRecipe(this);                   // 🔴 역방향(주인) 세팅 필수
            this.ingredients.add(ri);
        }
    }

    @Lob
    @Column(columnDefinition = "TEXT")
    private String alternativeIngredients; // JSON 형식의 대체 재료

    @Lob
    @Column(columnDefinition = "TEXT")
    private String handlingMethods; // JSON 형식의 처리 방법 추가

    @Lob
    @Column(columnDefinition = "TEXT")
    private String cookingSteps; // JSON 형식의 조리 순서

    private String mainImageUrl; // 대표 사진 URL

    @Enumerated(EnumType.STRING)
    private RecipeDifficulty difficulty; // 난이도 (ENUM)

    private String tags; //태그
    private int cookingTime; // 소요시간 (분 단위)
    private int servings; // 인원수
    private LocalDateTime createdAt; // 생성 일시
    private boolean isPublic; // 공개

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int likes = 0;

    @Column(nullable = false)
    private int recommends = 0;

    private String videoUrl;

    private int likePointStep = 0;   // 좋아요 10개 단위 포인트 지급 추적
    private int scrapPointStep = 0;  // 찜 10개 단위 포인트 지급 추적


    @Column(nullable = false)
    private boolean isDraft = false;   // 임시저장 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeType recipeType;     // 작성 타입 (IMAGE / VIDEO / BOTH)
}