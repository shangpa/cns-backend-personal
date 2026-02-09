package com.example.springjwt.ingredient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {
    Page<IngredientMaster> findByCategory(IngredientCategory category, Pageable pageable);
    Page<IngredientMaster> findByCategoryAndNameKoContainingIgnoreCase(
            IngredientCategory category, String keyword, Pageable pageable);
    Optional<IngredientMaster> findByNameKoIgnoreCase(String nameKo);
    List<IngredientMaster> findTop20ByNameKoContainingIgnoreCaseOrderByNameKoAsc(String keyword);
}
