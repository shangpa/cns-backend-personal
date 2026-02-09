package com.example.springjwt.admin.dto;

import com.example.springjwt.report.Report;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReportResponseDTO {
    private Long reportId;
    private String reporterUsername;
    private String reportedContent; // 게시글 내용 or 댓글 내용
    private String targetType; // "게시글" or "댓글"
    private LocalDateTime createdAt;

    public static ReportResponseDTO fromBoardReport(Report report) {
        return new ReportResponseDTO(
                report.getId(),
                report.getReporter().getUsername(),
                report.getBoard().getContent(),
                "게시글",
                report.getCreatedAt()
        );
    }

    public static ReportResponseDTO fromCommentReport(Report report) {
        return new ReportResponseDTO(
                report.getId(),
                report.getReporter().getUsername(),
                report.getBoardComment().getContent(),
                "댓글",
                report.getCreatedAt()
        );
    }
}
