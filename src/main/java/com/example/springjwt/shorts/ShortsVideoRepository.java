package com.example.springjwt.shorts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShortsVideoRepository extends JpaRepository<ShortsVideo, Long> {
    List<ShortsVideo> findTop10ByIsPublicTrueOrderByCreatedAtDesc(); // 최신순
    List<ShortsVideo> findTop10ByIsPublicTrueOrderByViewCountDesc(); // 인기순
    int countByUser_Id(int userId);

    @Query(value = "SELECT * FROM shorts_video WHERE is_public = true ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<ShortsVideo> findRandomSimple(@Param("limit") int limit);

    // ✅ 위치 파라미터 사용 (MySQL): seed, offset, limit
    @Query(
            value = """
            SELECT * FROM shorts_video
            WHERE is_public = true
            ORDER BY CRC32(CONCAT(?1, id))
            LIMIT ?2, ?3
            """,
            nativeQuery = true
    )
    List<ShortsVideo> findRandomBySeedPositional(
            String seed,   // ?1
            int offset,    // ?2
            int limit      // ?3
    );

    Page<ShortsVideo> findByUser_IdAndIsPublicTrue(int userId, Pageable pageable);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShortsVideo s set s.viewCount = s.viewCount + 1 where s.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // 공개된 쇼츠 검색 (JPQL) — 제목만 검색
    @Query("""
    SELECT s
    FROM ShortsVideo s
    WHERE s.isPublic = true
      AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY s.createdAt DESC
""")
    List<ShortsVideo> searchPublicByKeyword(@Param("keyword") String keyword);

}
