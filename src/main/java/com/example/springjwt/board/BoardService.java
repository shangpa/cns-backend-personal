package com.example.springjwt.board;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.admin.dto.*;
import com.example.springjwt.admin.log.AdminLogService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final AdminLogService adminLogService;

    // 작성
    public BoardResponseDTO create(BoardRequestDTO dto, String username) {
        UserEntity user = userRepository.findByUsername(username);
        Board board = new Board();
        board.setWriter(user);
        board.setContent(dto.getContent());
        board.setImageUrls(dto.getImageUrls());
        board.setBoardType(dto.getBoardType());

        Board saved = boardRepository.save(board);

        return new BoardResponseDTO(
                saved.getId(), saved.getContent(), user.getUsername(),
                saved.getImageUrls(), saved.getBoardType().toString(), saved.getCreatedAt()
        );
    }
    // 인기 Top 10
    public List<BoardDetailResponseDTO> getPopularBoards() {
        List<BoardType> types = List.of(BoardType.COOKING, BoardType.FREE);
        Pageable pageable = PageRequest.of(0, 10);

        List<Board> boards = boardRepository.findPopularBoards(types, pageable);

        return boards.stream().map(board -> new BoardDetailResponseDTO(
                board.getId(),
                board.getContent(),
                board.getWriter().getUsername(),
                board.getImageUrls(),
                board.getBoardType().name(),
                board.getCreatedAt().toString(),
                (int) boardLikeRepository.countByBoard(board),
                false,
                board.getCommentCount()
        )).toList();
    }

    //특정 id 조회
    public BoardDetailResponseDTO getBoardDetail(Long id, String username) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
        UserEntity user = userRepository.findByUsername(username);
        boolean liked = boardLikeRepository.existsByUserAndBoard(user, board);

        return new BoardDetailResponseDTO(
                board.getId(),
                board.getContent(),
                board.getWriter().getUsername(),
                board.getImageUrls(),
                board.getBoardType().toString(),
                board.getCreatedAt().toString(),
                board.getLikeCount(),
                liked,
                board.getCommentCount()
        );
    }

    // 인기게시판: 타입 상관없이 좋아요순 TOP N
    public List<BoardDetailResponseDTO> getPopularBoards(int limit, String username) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "likeCount"));
        List<Board> boards = boardRepository.findAll(pageable).getContent();

        return boards.stream()
                .map(board -> toDetailDTO(board, username))
                .toList();
    }
    // 타입별 최신순 TOP N
    public List<BoardDetailResponseDTO> getBoardsByTypeAndSort(BoardType type, String sort, int limit, String username) {
        Sort sorting = sort.equals("like") ?
                Sort.by(Sort.Direction.DESC, "likeCount") :
                Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, limit, sorting);
        List<Board> boards = boardRepository.findByBoardType(type, pageable).getContent();

        return boards.stream()
                .map(board -> toDetailDTO(board, username))
                .toList();
    }

    // 변환 함수
    private BoardDetailResponseDTO toDetailDTO(Board board, String username) {
        boolean liked = false;
        if (username != null) {
            liked = boardLikeRepository.existsByUserUsernameAndBoard(username, board);
        }
        return new BoardDetailResponseDTO(
                board.getId(),
                board.getContent(),
                board.getWriter().getUsername(),
                board.getImageUrls(),
                board.getBoardType().toString(),
                board.getCreatedAt().toString(),
                board.getLikeCount(),
                liked,
                board.getCommentCount()
        );
    }

    public List<BoardDetailResponseDTO> getBoardsByTypePaged(BoardType type, String sort, int page, int size, String username) {
        Sort sorting = switch (sort) {
            case "like" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "comment" -> Sort.by(Sort.Direction.DESC, "commentCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(page, size, sorting);
        List<Board> boards = boardRepository.findByBoardType(type, pageable).getContent();
        return boards.stream().map(b -> toDetailDTO(b, username)).toList();
    }
    
    //마이페이지 - 작성한 게시글 조회
    public List<BoardDetailResponseDTO> getBoardsByUser(String username) {
        UserEntity user = userRepository.findByUsername(username);
        List<Board> boards = boardRepository.findByWriter(user);
        return boards.stream().map(board -> BoardDetailResponseDTO.from(board)).toList();
    }

    //마이페이지 - 작성한 게시글 삭제
    public void deleteBoard(Long id, String username) {
        Board board = boardRepository.findById(id).orElseThrow();
        if (!board.getWriter().getUsername().equals(username)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }
        boardRepository.delete(board);
    }

    //마이페이지 - 작성한 게시글 수정
    public void updateBoard(Long id, BoardRequestDTO dto, String username) {
        Board board = boardRepository.findById(id).orElseThrow();
        if (!board.getWriter().getUsername().equals(username)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }
        board.setContent(dto.getContent());
        board.setBoardType(dto.getBoardType());
        board.setImageUrls(dto.getImageUrls()); // 필요 시 처리
        boardRepository.save(board);
    }

    //관리자용 인기순3개
    public List<BoardDetailResponseDTO> getTop3PopularBoardsForAdmin() {
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "likeCount"));
        List<Board> boards = boardRepository.findAll(pageable).getContent();

        return boards.stream()
                .map(board -> new BoardDetailResponseDTO(
                        board.getId(),
                        board.getContent(),
                        board.getWriter().getUsername(),
                        board.getImageUrls(),
                        board.getBoardType().toString(),
                        board.getCreatedAt().toString(),
                        board.getLikeCount(),
                        false,
                        board.getCommentCount()
                )).toList();
    }
    public List<BoardMonthlyStatsDTO> countBoardMonthly(LocalDateTime startDate) {
        List<Object[]> rawData = boardRepository.countBoardMonthlyRaw(startDate);
        return rawData.stream()
                .map(row -> new BoardMonthlyStatsDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue() // COUNT는 Long이지만 안전하게 처리
                ))
                .collect(Collectors.toList());
    }
    public Page<BoardAdminListResponseDTO> getBoards(Pageable pageable) {
        return boardRepository.findAllBoardsForAdmin(pageable);
    }

    public BoardDetailAdminDTO getBoardDetail(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        List<BoardCommentResponseDTO> commentDTOs = board.getComments().stream()
                .map(c -> new BoardCommentResponseDTO(
                        c.getId(),
                        c.getUser().getUsername(),
                        c.getContent(),
                        c.getCreatedAt()
                ))
                .toList();

        return new BoardDetailAdminDTO(
                board.getId(),
                board.getWriter().getUsername(),
                board.getContent(),
                board.getImageUrls(),
                board.getLikeCount(),
                board.getCommentCount(),
                board.getCreatedAt(),
                board.getBoardType(),
                commentDTOs
        );
    }

    @Transactional
    public void deleteBoardByAdmin(Long boardId, String adminUsername, String reason) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 1. 댓글 먼저 삭제
        boardCommentRepository.deleteAllByBoard(board);

        // 2. 좋아요 삭제
        boardLikeRepository.deleteAllByBoard(board);

        // 3. 게시글 삭제
        boardRepository.delete(board);

        // 4. 관리자 로그 저장
        adminLogService.logAdminAction(
                adminUsername,
                "DELETE_BOARD",
                "BOARD",
                boardId,
                reason
        );
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId, String adminUsername, String reason) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        Board board = comment.getBoard();
        board.setCommentCount(Math.max(0, board.getCommentCount() - 1)); // 안전하게 감소

        boardCommentRepository.delete(comment);

        adminLogService.logAdminAction(
                adminUsername,
                "DELETE_COMMENT",
                "BOARD_COMMENT",
                commentId,
                reason
        );
    }
    public Page<CommentAdminResponseDTO> getCommentsForAdmin(Pageable pageable) {
        return boardCommentRepository.findAll(pageable)
                .map(CommentAdminResponseDTO::from);
    }

    public Page<CommentAdminResponseDTO> searchCommentsByContent(String keyword, Pageable pageable) {
        return boardCommentRepository.findByContentContaining(keyword, pageable)
                .map(CommentAdminResponseDTO::from);
    }
    // BoardService.java
    public Long getBoardIdByCommentId(Long commentId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        return comment.getBoard().getId(); // Board 엔티티에서 id 꺼냄
    }
}