package com.example.springjwt.fridge.history;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.springjwt.User.UserEntity;

import java.util.List;

public interface FridgeHistoryRepository extends JpaRepository<FridgeHistory, Long> {
    List<FridgeHistory> findByUserAndIngredientNameOrderByActionDateDesc(UserEntity user, String ingredientName);
    List<FridgeHistory> findAllByUser(UserEntity user);
}