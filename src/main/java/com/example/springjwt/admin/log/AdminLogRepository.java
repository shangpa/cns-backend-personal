package com.example.springjwt.admin.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    @Query("SELECT l FROM AdminLog l WHERE l.targetType = 'USER' AND l.targetId = :userId AND l.action = 'BLOCK_USER' ORDER BY l.createdAt DESC")
    List<AdminLog> findRecentUserBlocks(@Param("userId") int userId);
    @Query("SELECT l FROM AdminLog l WHERE l.action LIKE 'DELETE_BOARD' ORDER BY l.createdAt DESC")
    Page<AdminLog> findDeletedLogs(Pageable pageable);
    @Query("SELECT l FROM AdminLog l WHERE l.action = 'DELETE_COMMENT' ORDER BY l.createdAt DESC")
    Page<AdminLog> findDeletedCommentLogs(Pageable pageable);
    @Query("SELECT l FROM AdminLog l WHERE l.targetType = 'RECIPE' AND l.action = 'DELETE_RECIPE' ORDER BY l.createdAt DESC")
    Page<AdminLog> findDeletedRecipeLogs(Pageable pageable);
    @Query("SELECT l FROM AdminLog l WHERE l.targetType = 'RECIPE_REVIEW' AND l.action = 'DELETE_RECIPE_REVIEW' ORDER BY l.createdAt DESC")
    Page<AdminLog> findDeletedRecipeReviewLogs(Pageable pageable);

}