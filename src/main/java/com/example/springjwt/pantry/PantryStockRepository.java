package com.example.springjwt.pantry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.springjwt.pantry.StorageLocation;

public interface PantryStockRepository extends JpaRepository<PantryStock, Long> {
    List<PantryStock> findAllByPantry_Id(Long pantryId);
    List<PantryStock> findAllByPantry_IdAndIngredient_Id(Long pantryId, Long ingredientId);
    @Query("select count(s) from PantryStock s where s.pantry.id=:pantryId")
    Long countNow(@Param("pantryId") Long pantryId);

    @Query("""
    select
      sum(case when s.storage = com.example.springjwt.pantry.StorageLocation.FRIDGE  then 1 else 0 end),
      sum(case when s.storage = com.example.springjwt.pantry.StorageLocation.FREEZER then 1 else 0 end),
      sum(case when s.storage = com.example.springjwt.pantry.StorageLocation.PANTRY  then 1 else 0 end)
    from PantryStock s
    where s.pantry.id = :pantryId
    """)
    Object[] countByStorage(@Param("pantryId") Long pantryId);

    /** 현재 보유 재고(수량>0) 중 '오늘 이전'에 만료된 재고 개수 */
    @Query("""
    select count(s)
      from PantryStock s
     where s.pantry.id = :pantryId
       and s.quantity > 0
       and s.expiresAt is not null
       and s.expiresAt < :today
    """)
    Long countExpiredStocks(@Param("pantryId") Long pantryId,
                            @Param("today") java.time.LocalDate today);

    /** 현재 보유 재고(수량>0)를 카테고리별로 집계 */
    @Query("""
    select s.ingredient.category, count(s)
      from PantryStock s
     where s.pantry.id = :pantryId
       and s.quantity > 0
     group by s.ingredient.category
    """)
    List<Object[]> countByCategory(@Param("pantryId") Long pantryId);

    Optional<PantryStock> findByPantry_IdAndIngredient_IdAndUnit_IdAndStorageAndPurchasedAtAndExpiresAt(
            Long pantryId,
            Long ingredientId,
            Long unitId,
            StorageLocation storage,
            LocalDate purchasedAt,
            LocalDate expiresAt
    );
}
