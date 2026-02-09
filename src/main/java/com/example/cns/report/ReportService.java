package com.example.cns.report;

import com.example.cns.User.UserEntity;
import com.example.cns.User.UserRepository;
import com.example.cns.admin.dto.BoardMonthlyStatsDTO;
import com.example.cns.admin.dto.ReportResponseDTO;
import com.example.cns.board.Board;
import com.example.cns.board.BoardComment;
import com.example.cns.board.BoardCommentRepository;
import com.example.cns.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final UserRepository userRepository;

    public void report(ReportRequestDTO dto, String username) {
        UserEntity reporter = userRepository.findByUsername(username);
        Report report = new Report();
        report.setReporter(reporter);

        if (dto.getBoardId() != null) {
            Board board = boardRepository.findById(dto.getBoardId()).orElseThrow();
            report.setBoard(board);
        }
        if (dto.getBoardCommentId() != null) {
            BoardComment comment = boardCommentRepository.findById(dto.getBoardCommentId()).orElseThrow();
            report.setBoardComment(comment);
        }
        reportRepository.save(report);
    }

    public List<BoardMonthlyStatsDTO> countReportMonthly(LocalDateTime startDate) {
        List<Object[]> rawData = reportRepository.countReportMonthlyRaw(startDate);

        return rawData.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
    public Page<ReportResponseDTO> getBoardReports(Pageable pageable) {
        return reportRepository.findByBoardCommentIsNull(pageable)
                .map(ReportResponseDTO::fromBoardReport);
    }

    public Page<ReportResponseDTO> getCommentReports(Pageable pageable) {
        return reportRepository.findByBoardCommentIsNotNull(pageable)
                .map(ReportResponseDTO::fromCommentReport);
    }

    public Page<ReportResponseDTO> searchBoardReports(String keyword, Pageable pageable) {
        return reportRepository.searchBoardReports(keyword, pageable)
                .map(ReportResponseDTO::fromBoardReport);
    }

    public Page<ReportResponseDTO> searchCommentReports(String keyword, Pageable pageable) {
        return reportRepository.searchCommentReports(keyword, pageable)
                .map(ReportResponseDTO::fromCommentReport);
    }
}