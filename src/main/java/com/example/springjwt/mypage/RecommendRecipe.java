package com.example.springjwt.mypage;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipe.Recipe;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Recipe recipe;

    private LocalDateTime recommendedAt = LocalDateTime.now();
}
