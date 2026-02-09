package com.example.springjwt.pantry.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PantryHistoryRepository extends JpaRepository<PantryHistory, Long> {

    @EntityGraph(attributePaths = {"ingredient","unit","stock"})
    List<PantryHistory> findByPantry_IdOrderByCreatedAtDesc(Long pantryId);

    @EntityGraph(attributePaths = {"ingredient","unit","stock"})
    List<PantryHistory> findByPantry_IdAndIngredient_NameKoContainingIgnoreCaseOrderByCreatedAtDesc(
            Long pantryId, String keyword
    );

    // 만료 전 소비 = eaten
    @Query("""
    select count(h)
      from PantryHistory h
     where h.action = com.example.springjwt.pantry.history.HistoryAction.USE
       and h.pantry.id = :pantryId
       and h.createdAt between :from and :to
       and h.stock is not null
       and h.stock.expiresAt is not null
       and h.createdAt <= h.stock.expiresAt
    """)
    Long countEatenInPeriod(@Param("pantryId") Long pantryId,
                            @Param("from") LocalDateTime from,
                            @Param("to")   LocalDateTime to);

    // 만료 못 지킨 수: “만료 후”에 소비(USE) 또는 폐기(DISCARD)
    @Query("""
    select count(h)
      from PantryHistory h
     where h.pantry.id = :pantryId
       and h.createdAt between :from and :to
       and h.stock is not null
       and h.stock.expiresAt is not null
       and (
            (h.action = com.example.springjwt.pantry.history.HistoryAction.USE     and h.createdAt > h.stock.expiresAt)
         or (h.action = com.example.springjwt.pantry.history.HistoryAction.DISCARD and h.createdAt > h.stock.expiresAt)
       )
    """)
    Long countExpiredInPeriod(@Param("pantryId") Long pantryId,
                              @Param("from") LocalDateTime from,
                              @Param("to")   LocalDateTime to);

    // 이번 달 Top-소비 재료 (이름, 총 소비량) — changeQty가 음수라서 -1 * changeQty로 합산
    @Query("""
    select h.ingredient.nameKo as name, sum((-1) * h.changeQty) as total
      from PantryHistory h
     where h.pantry.id = :pantryId
       and h.action = com.example.springjwt.pantry.history.HistoryAction.USE
       and h.createdAt between :from and :to
     group by h.ingredient.nameKo
     order by total desc
    """)
    List<Object[]> topConsumedInPeriod(@Param("pantryId") Long pantryId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to")   LocalDateTime to);

}
