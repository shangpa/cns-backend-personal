package com.example.springjwt.board;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.admin.dto.BoardAdminListResponseDTO;
import com.example.springjwt.admin.dto.BoardMonthlyStatsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByBoardType(BoardType boardType);

    //좋아요순 기준
    @Query("SELECT b FROM Board b WHERE b.boardType IN (:types) ORDER BY b.likeCount DESC")
    List<Board> findPopularBoards(@Param("types") List<BoardType> types, Pageable pageable);
    //보드 타입별로 페이지수 정렬
    Page<Board> findByBoardType(BoardType boardType, Pageable pageable);

    @Query("SELECT b FROM Board b LEFT JOIN b.comments c WHERE b.boardType = :type GROUP BY b ORDER BY COUNT(c) DESC")
    List<Board> findBoardsByCommentCount(@Param("type") BoardType type, Pageable pageable);

    List<Board> findByWriter(UserEntity user);

    // com.example.springjwt.board.BoardRepository
    @Query("""
    SELECT FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m'), COUNT(b)
    FROM Board b
    WHERE b.createdAt >= :startDate
    GROUP BY FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m')
""")
    List<Object[]> countBoardMonthlyRaw(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT new com.example.springjwt.admin.dto.BoardAdminListResponseDTO(b.id, b.writer.username, b.content, b.createdAt) " +
            "FROM Board b")
    Page<BoardAdminListResponseDTO> findAllBoardsForAdmin(Pageable pageable);

}
