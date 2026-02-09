package com.example.springjwt.search;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    @Query("""
    SELECT k.keyword, COUNT(k) 
    FROM SearchKeyword k
    WHERE k.searchedAt >= :since
    GROUP BY k.keyword
    ORDER BY COUNT(k) DESC
    """)
    List<Object[]> findTopKeywordsSince(LocalDateTime since, Pageable pageable);
}
