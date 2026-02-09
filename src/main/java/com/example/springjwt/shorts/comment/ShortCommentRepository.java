package com.example.springjwt.shorts.comment;


import org.springframework.data.jpa.repository.JpaRepository;
import com.example.springjwt.shorts.ShortsVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShortCommentRepository extends JpaRepository<ShortComment, Long> {
    List<ShortComment> findByShortsVideoIdOrderByCreatedAtAsc(Long ShortsVideoId);

    @Query("""
    SELECT FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m'), COUNT(c)
    FROM ShortComment c
    WHERE c.createdAt >= :startDate
    GROUP BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', c.createdAt, '%Y-%m')
""")

    List<Object[]> countCommentMonthly(@Param("startDate") LocalDateTime startDate);

    void deleteAllByShortsVideo(ShortsVideo shortsVideo);

    Page<ShortComment> findByContentContaining(String keyword, Pageable pageable);
}
