package com.example.springjwt.report;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {
    private Long boardId;         // 게시글 신고 시
    private Long boardCommentId;  // 댓글 신고 시

}