package com.example.springjwt.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // Optional<Report> findByReporterAndBoard(UserEntity reporter, Board board);
    // Optional<Report> findByReporterAndBoardComment(UserEntity reporter, BoardComment boardComment);


    @Query("""
    SELECT FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m'), COUNT(r)
    FROM Report r
    WHERE r.createdAt >= :startDate
    GROUP BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m')
""")
    List<Object[]> countReportMonthlyRaw(@Param("startDate") LocalDateTime startDate);

    // 게시글 신고만 조회
    @Query("SELECT r FROM Report r WHERE r.board IS NOT NULL AND r.boardComment IS NULL AND " +
            "(r.board.content LIKE %:keyword% OR r.reporter.username LIKE %:keyword%)")
    Page<Report> searchBoardReports(@Param("keyword") String keyword, Pageable pageable);

    // 댓글 신고만 조회
    @Query("SELECT r FROM Report r WHERE r.boardComment IS NOT NULL AND " +
            "(r.boardComment.content LIKE %:keyword% OR r.reporter.username LIKE %:keyword%)")
    Page<Report> searchCommentReports(@Param("keyword") String keyword, Pageable pageable);

    // 전체 페이징
    Page<Report> findByBoardCommentIsNull(Pageable pageable); // 게시글 신고
    Page<Report> findByBoardCommentIsNotNull(Pageable pageable); // 댓글 신고
}