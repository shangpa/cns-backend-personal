package com.example.springjwt.tradepost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.admin.dto.UserTradePostSimpleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradePostRepository extends JpaRepository<TradePost, Long> {

    List<TradePost> findByUser(UserEntity user);

    List<TradePost> findByCategory(String category);

    List<TradePost> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleKeyword,
            String descriptionKeyword
    );

    // (레거시) 사용자 위치 기준 거리 필터링 - 네이티브 예시
    @Query(value = """
        SELECT *,
        (6371 * acos(
            cos(radians(:lat)) * cos(radians(tp.latitude)) *
            cos(radians(tp.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(tp.latitude))
        )) AS distance
        FROM trade_post tp
        HAVING distance <= :distanceKm
        ORDER BY distance
    """, nativeQuery = true)
    List<TradePost> findNearbyTradePosts(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("distanceKm") double distanceKm
    );

    // (레거시) 카테고리 + 거리순 필터링 - 단일 카테고리
    @Query("""
        SELECT t FROM TradePost t
        WHERE t.latitude IS NOT NULL AND t.longitude IS NOT NULL
          AND t.category = :category
          AND (
            6371 * acos(
              cos(radians(:userLat)) *
              cos(radians(t.latitude)) *
              cos(radians(t.longitude) - radians(:userLon)) +
              sin(radians(:userLat)) *
              sin(radians(t.latitude))
            )
          ) <= :distanceKm
    """)
    List<TradePost> findNearbyByCategory(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("distanceKm") double distanceKm,
            @Param("category") String category
    );

    @Query("SELECT t FROM TradePost t ORDER BY t.viewCount DESC")
    List<TradePost> findTop3ByOrderByViewCountDesc(Pageable pageable);

    @Query("SELECT t FROM TradePost t ORDER BY t.createdAt DESC")
    List<TradePost> findAllByOrderByCreatedAtDesc();

    List<TradePost> findByBuyerAndStatus(UserEntity buyer, int status);

    List<TradePost> findByUser_Username(String username);

    List<TradePost> findByUser_UsernameAndStatus(String username, int status);

    // 전체 거래글 월별 수
    @Query("""
        SELECT FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m'), COUNT(t)
        FROM TradePost t
        WHERE t.createdAt >= :startDate
        GROUP BY FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m')
    """)
    List<Object[]> countTradePostMonthlyRaw(@Param("startDate") LocalDateTime startDate);

    // 가격이 0원인 거래글 월별 수
    @Query("""
        SELECT FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m'), COUNT(t)
        FROM TradePost t
        WHERE t.createdAt >= :startDate AND t.price = 0
        GROUP BY FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m')
    """)
    List<Object[]> countFreeTradePostMonthlyRaw(@Param("startDate") LocalDateTime startDate);

    int countByUser(UserEntity user);

    Page<TradePost> findAll(Pageable pageable);

    Page<TradePost> findByStatus(int status, Pageable pageable);

    @Query("SELECT new com.example.springjwt.admin.dto.UserTradePostSimpleDTO(" +
            "t.tradePostId, t.title, " +
            "CASE WHEN LENGTH(t.description) > 15 THEN CONCAT(SUBSTRING(t.description, 1, 15), '...') ELSE t.description END, " +
            "t.createdAt, t.status) " +
            "FROM TradePost t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<UserTradePostSimpleDTO> findSalesByUserId(@Param("userId") int userId);

    @Query("SELECT new com.example.springjwt.admin.dto.UserTradePostSimpleDTO(" +
            "t.tradePostId, t.title, " +
            "CASE WHEN LENGTH(t.description) > 15 THEN CONCAT(SUBSTRING(t.description, 1, 15), '...') ELSE t.description END, " +
            "t.createdAt, t.status) " +
            "FROM TradePost t WHERE t.buyer.id = :userId ORDER BY t.createdAt DESC")
    List<UserTradePostSimpleDTO> findPurchasesByUserId(@Param("userId") int userId);

    @Query("""
        SELECT t FROM TradePost t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<TradePost> findByStatusAndTitleKeyword(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 소유자까지 fetch해서 소유권 검증 최적화
    @Query("SELECT t FROM TradePost t JOIN FETCH t.user u WHERE t.tradePostId = :postId")
    Optional<TradePost> findWithOwner(@Param("postId") Long postId);

    // 최신 노출순(UP 반영)
    @Query("SELECT t FROM TradePost t ORDER BY t.updatedAt DESC")
    List<TradePost> findAllByOrderByUpdatedAtDesc();

    @Query(value = """
    SELECT tp.*
    FROM trade_post tp
    WHERE (:status IS NULL OR tp.status = :status)
      AND (:categoriesEmpty = TRUE OR tp.category IN (:categories))
      AND (
           :distanceKm IS NULL
           OR (
                tp.latitude IS NOT NULL AND tp.longitude IS NOT NULL
                AND (
                    6371.0 * acos(
                        cos(radians(:lat)) * cos(radians(tp.latitude)) *
                        cos(radians(tp.longitude) - radians(:lng)) +
                        sin(radians(:lat)) * sin(radians(tp.latitude))
                    )
                ) <= :distanceKm
           )
      )
    ORDER BY tp.createdAt DESC
    """, nativeQuery = true)
    List<TradePost> findNearbyFlexible(
            @Param("lat")  Double lat,
            @Param("lng")  Double lng,
            @Param("distanceKm") Double distanceKm,
            @Param("categories") List<String> categories,
            @Param("categoriesEmpty") boolean categoriesEmpty,
            @Param("status") Integer status
    );

}
