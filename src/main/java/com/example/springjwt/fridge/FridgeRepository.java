package com.example.springjwt.fridge;

import com.example.springjwt.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FridgeRepository extends JpaRepository<Fridge, Long> {
    //조회기준
    List<Fridge> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByUser(UserEntity user);

    //삭제
    List<Fridge> findAllByUserAndIngredientName(UserEntity user, String ingredientName);
    
    // 냉장고 재료 차감 또는 예상 재료 조회용 (createdAt 오래된 순 정렬)
    List<Fridge> findAllByUserAndIngredientNameOrderByCreatedAtAsc(UserEntity user, String ingredientName);

    // 최신 보유 재료 정보 (예: 가장 최근에 구매한 재료 정보가 필요할 경우)
    Optional<Fridge> findTopByUserAndIngredientNameOrderByUpdatedAtDesc(UserEntity user, String ingredientName);
}
