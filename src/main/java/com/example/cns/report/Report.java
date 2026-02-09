package com.example.cns.report;

import com.example.cns.User.UserEntity;
import com.example.cns.board.Board;
import com.example.cns.board.BoardComment;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Report {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private UserEntity reporter;

    @ManyToOne
    private Board board;
    @ManyToOne
    private BoardComment boardComment; // 댓글 신고시 (nullable)

    private LocalDateTime createdAt = LocalDateTime.now();
}