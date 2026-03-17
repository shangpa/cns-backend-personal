package com.example.cns.admin;

import com.example.cns.User.JoinService;
import com.example.cns.User.UserEntity;
import com.example.cns.User.UserRepository;
import com.example.cns.admin.dto.*;
import com.example.cns.admin.enums.StatType;
import com.example.cns.admin.log.AdminLog;
import com.example.cns.admin.log.AdminLogRepository;
import com.example.cns.board.BoardDetailResponseDTO;
import com.example.cns.board.BoardService;
import com.example.cns.dto.CustomUserDetails;
import com.example.cns.dto.JoinDTO;
import com.example.cns.admin.dto.PointHistoryDTO;
import com.example.cns.ingredient.IngredientCategory;
import com.example.cns.ingredient.UnitEntity;
import com.example.cns.ingredient.UnitRepository;
import com.example.cns.ingredient.dto.IngredientMasterResponse;
import com.example.cns.mypage.LikeRecipeRepository;
import com.example.cns.mypage.RecommendRecipeRepository;
import com.example.cns.point.PointService;
import com.example.cns.recipe.RecipeRepository;
import com.example.cns.recipe.RecipeSearchResponseDTO;
import com.example.cns.recipe.RecipeStatService;
import com.example.cns.report.ReportService;
import com.example.cns.review.Recipe.ReviewRepository;
import com.example.cns.review.Recipe.ReviewService;
import com.example.cns.review.TradePost.TpReviewRepository;
import com.example.cns.tradepost.TradePostRepository;
import com.example.cns.tradepost.TradePostService;
import com.example.cns.tradepost.TradePostSimpleResponseDTO;
import com.example.cns.tradepost.TradeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JoinService joinService;
    private final RecipeStatService recipeStatService;
    private final AdminRecipeService adminRecipeService;
    private final TradePostService tradePostService;
    private final BoardService boardService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final TradePostRepository tradePostRepository;
    private final ReviewRepository reviewRepository;
    private final PointService pointService;
    private final AdminService adminService;
    private final TpReviewRepository tpReviewRepository;
    private final AdminLogRepository adminLogRepository;
    private final LikeRecipeRepository likeRecipeRepository;
    private final RecommendRecipeRepository recommendRecipeRepository;
    private final ReviewService reviewService;
    private final AdminIngredientService adminIngredientService;
    private final UnitRepository unitRepository;

    // 관리자 회원가입
    @PostMapping("/join")
    public ResponseEntity<String> adminJoin(@RequestBody JoinDTO joinDTO) {
        boolean success = joinService.joinAdminProcess(joinDTO);
        if (success) {
            return ResponseEntity.ok("✅ 관리자 회원가입 성공");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 관리자 아이디입니다");
        }
    }

    // 테스트용 관리자 전용 API
    @GetMapping("/test")
    public ResponseEntity<String> adminOnlyApi() {
        log.debug("관리자 :{}로그인", Thread.currentThread().getName());
        return ResponseEntity.ok("✅ 관리자 전용 API 접근 성공");
    }
    // 관리자 인기 레시피 3개 조회
    @GetMapping("/recipes/top3")
    public ResponseEntity<List<RecipeSearchResponseDTO>> getTop3Recipes() {
        return ResponseEntity.ok(adminRecipeService.getTop3Recipes());
    }

    //최근 4개월 동안
    @GetMapping("/monthly-stats")
    public ResponseEntity<List<RecipeMonthlyStatsDTO>> getMonthlyStats() {
        return ResponseEntity.ok(recipeStatService.getRecentFourMonthsStats());
    }
    
    // 관리자 인기 거래글 3개 조회
    @GetMapping("/popular/top3")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPopularTradePosts() {
        List<TradePostSimpleResponseDTO> topPosts = tradePostService.getTop3PopularTradePosts();
        return ResponseEntity.ok(topPosts);
    }

    // 관리자 인기 커뮤니티 3개 조회
    @GetMapping("/boards/top3")
    public ResponseEntity<List<BoardDetailResponseDTO>> getTop3Boards() {
        return ResponseEntity.ok(boardService.getTop3PopularBoardsForAdmin());
    }

    //최근 4개월 커뮤니티 게시글 통계
    @GetMapping("/board/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getBoardMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<BoardMonthlyStatsDTO> stats = boardService.countBoardMonthly(startDate);
        return ResponseEntity.ok(stats);
    }

    //최근 4개월 커뮤니티 댓글 통계
    @GetMapping("/comment/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getCommentMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        List<BoardMonthlyStatsDTO> stats = adminRecipeService.countCommentMonthly(startDate);
        return ResponseEntity.ok(stats);
    }

    //최근 4개월 신고 통계
    @GetMapping("/report/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getReportMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<BoardMonthlyStatsDTO> stats = reportService.countReportMonthly(startDate);
        return ResponseEntity.ok(stats);
    }

    //최근 4개월 레시피 통계
    @GetMapping("/recipe/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getRecipeMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return ResponseEntity.ok(recipeStatService.countRecipeMonthly(startDate));
    }

    //최근 4개월 레시피 조회수 통계
    @GetMapping("/recipe/views/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getRecipeViewsMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return ResponseEntity.ok(recipeStatService.sumRecipeViewsMonthly(startDate));
    }

    // 최근 4개월 전체 거래글 통계
    @GetMapping("/trade/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getTradePostMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return ResponseEntity.ok(tradePostService.countTradePostMonthly(startDate));
    }

    // 최근 4개월 무료 거래글 통계
    @GetMapping("/trade/free/monthly")
    public ResponseEntity<List<BoardMonthlyStatsDTO>> getFreeTradePostMonthlyStats() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusMonths(3)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return ResponseEntity.ok(tradePostService.countFreeTradePostMonthly(startDate));
    }

    /**
     * 관리자용 회원 리스트 조회 (페이징)
     * - 응답: 회원 id, 이름(name), 아이디(username)
     * - GET /admin/users?page=0&size=10
     */
    @GetMapping("/users")
    public Page<UserListDTO> getUserList(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return userRepository.findByRoleNotAndBlockedFalse("ADMIN",
                        PageRequest.of(page, size, Sort.by("id").descending()))
                .map(user -> new UserListDTO(
                        user.getId(),
                        user.getName(),
                        user.getUsername()
                ));
    }
    
    /**
     * 관리자용 회원 검색 조회 (페이징)
     * - 응답: 회원 id, 이름(name), 아이디(username)
     * - GET /admin/users/search?keyword=홍&page=0&size=10
     */
    @GetMapping("/users/search")
    public Page<UserListDTO> searchUsers(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return userRepository.searchUsersExcludingAdminAndBlocked(keyword, pageable)
                .map(user -> new UserListDTO(
                        user.getId(),
                        user.getName(),
                        user.getUsername()
                ));
    }

    /**
     * 관리자용 회원 상세 정보 조회
     * - 응답: 이름, 아이디, 가입일, 포인트, 작성한 레시피 수, 거래글 수, 리뷰 수
     * - GET /admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public UserDetailDTO getUserDetail(@PathVariable int userId){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));

        int recipeCount = recipeRepository.countByUser(user);
        int tradePostCount = tradePostRepository.countByUser(user);
        int reviewCount = reviewRepository.countByUser(user);

        return new UserDetailDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getPoint(),
                recipeCount,
                tradePostCount,
                reviewCount
        );
    }

    /**
     * 특정 회원이 작성한 레시피 리스트 조회 (페이징)
     * - 응답: username, 레시피 제목, 작성일
     * - GET /admin/users/{userId}/recipes?page=0&size=10
     */
    @GetMapping("/users/{userId}/recipes")
    public Page<UserRecipeSimpleDTO> getUserRecipes(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return recipeRepository.findRecipesByUserId(userId, pageable);
    }

    /**
     * 특정 회원이 작성한 레시피 중 제목으로 검색 (페이징)
     * - GET /admin/users/{userId}/recipes/search?keyword=된장&page=0&size=10
     * - 응답: username, 레시피 제목, 작성일
     */
    @GetMapping("/users/{userId}/recipes/search")
    public Page<UserRecipeSimpleDTO> searchUserRecipesByTitle(
            @PathVariable int userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return recipeRepository.findRecipesByUserIdAndTitleContains(userId, keyword, pageable);
    }

    /**
     * [GET] /admin/tradeposts?page=0&size=10&status=ONGOING&sortBy=createdAt
     * 전체 거래글 조회 (status: ONGOING=거래중, COMPLETED=거래완료, 생략 시 전체)
     * 정렬 기준: createdAt, category 등 (기본값: createdAt 내림차순)
     * 응답: id, username, title, createdAt, category, status 포함
     */
    @GetMapping("/tradeposts")
    public Page<TradePostListResponseDTO> getTradePostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String keyword
    ) {
        return tradePostService.getTradePosts(page, size, status, sortBy, keyword);
    }


    /**
     * [GET] /admin/tradeposts/{postId}
     * 거래글 상세 조회
     * 응답: id, username, title, description, createdAt, imageUrls, location, chatCount, viewCount 포함
     */
    @GetMapping("/tradeposts/{postId}")
    public ResponseEntity<TradePostDetailResponseDTO> getTradePostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(tradePostService.getTradePostDetail(postId));
    }

    /**
     * [관리자용 거래글 삭제 API]
     *
     * 거래글을 삭제하면서 삭제한 관리자 ID와 사유를 함께 전달받아 로그로 기록합니다.
     *
     * 요청 방식: DELETE
     * 요청 URL: /admin/tradeposts/{postId}
     * 요청 바디:
     * {
     *   "adminUsername": "admin01",
     *   "reason": "허위 게시글로 판단되어 삭제"
     * }
     * 응답: "삭제 및 로그 기록 완료"
     */
    @DeleteMapping("/tradeposts/{postId}")
    public ResponseEntity<String> deleteTradePostAsAdmin(
            @PathVariable Long postId,
            @RequestBody DeleteRequestDTO requestDTO
    ) {
        tradePostService.deletePostByAdmin(postId, requestDTO.getAdminUsername(), requestDTO.getReason());
        return ResponseEntity.ok("삭제 및 로그 기록 완료");
    }

    // [GET] /admin/boards
    // 관리자용 커뮤니티 게시글 조회 (페이징+정렬)
    // - page: 페이지 번호 (기본 0)
    // - size: 페이지 크기 (기본 10)
    // - sortBy: 정렬 기준 (기본 createdAt)
    // 응답: id, 작성자, 내용, 게시날짜 포함
    @GetMapping("boards")
    public Page<BoardAdminListResponseDTO> getBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return boardService.getBoards(pageRequest);
    }


    /* [GET] /admin/boards/{boardId}
     관리자용 게시글 상세 조회 API
     - boardId: 게시글 ID (PathVariable)
     - 댓글 리스트 포함
     - 응답: id, 작성자, 내용, 이미지, 날짜, 좋아요수, 댓글수, 댓글들
    */
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<BoardDetailAdminDTO> getBoardDetail(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoardDetail(boardId));
    }

    /**
     * [DELETE] /admin/boards/{boardId}
     * 관리자 커뮤니티 게시글 삭제
     * - 요청: 관리자 아이디(adminUsername), 삭제 사유(reason)
     * - 응답: "삭제 및 로그 기록 완료"
     */
    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<String> deleteBoardAsAdmin(
            @PathVariable Long boardId,
            @RequestBody DeleteRequestDTO requestDTO
    ) {
        boardService.deleteBoardByAdmin(boardId, requestDTO.getAdminUsername(), requestDTO.getReason());
        return ResponseEntity.ok("삭제 및 로그 기록 완료");
    }

    // [DELETE] /admin/comments/{commentId}
    // 관리자 댓글 삭제 (사유 기록 및 댓글수 감소 포함)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteCommentAsAdmin(
            @PathVariable Long commentId,
            @RequestBody DeleteRequestDTO requestDTO
    ) {
        boardService.deleteCommentByAdmin(commentId, requestDTO.getAdminUsername(), requestDTO.getReason());
        return ResponseEntity.ok("댓글 삭제 및 로그 기록 완료");
    }

    /**
     * [GET] /admin/comments
     * 관리자 댓글 리스트 조회 (페이징)
     * - page, size, sortBy 제공 가능
     */
    @GetMapping("/comments")
    public Page<CommentAdminResponseDTO> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return boardService.getCommentsForAdmin(pageable);
    }

    /**
     * [GET] /admin/comments/{commentId}/board
     * 특정 댓글이 달린 게시글 상세 조회
     * - commentId를 통해 연결된 게시글(boardId)을 찾아서 상세정보 반환
     * - 응답: BoardDetailAdminDTO (댓글 포함된 게시글 상세)
     */
    @GetMapping("/comments/{commentId}/board")
    public ResponseEntity<BoardDetailAdminDTO> getBoardByComment(@PathVariable Long commentId) {
        Long boardId = boardService.getBoardIdByCommentId(commentId);
        return ResponseEntity.ok(boardService.getBoardDetail(boardId));
    }


    /**
     * [GET] /admin/comments/search
     * @param page 조회할 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 레시피 개수 (기본값: 10)
     */
    @GetMapping("/comments/search")
    public Page<CommentAdminResponseDTO> searchComments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return boardService.searchCommentsByContent(keyword, pageable);
    }


    /**
     * 관리자용 레시피 리스트 조회 API
     * - 반환 필드: recipeId, username(작성자 아이디), title, createdAt(작성일시)
     *
     * @param page 조회할 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 레시피 개수 (기본값: 10)
     * @return Page<RecipeListAdminDTO> 형태로 페이징된 레시피 정보 반환
     */
    @GetMapping("/recipes")
    public ResponseEntity<Page<RecipeListAdminDTO>> getAllRecipesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminRecipeService.getRecipeListForAdmin(page, size));
    }

    /**
     * 관리자용 레시피 제목 검색 API
     * - 제목에 특정 키워드가 포함된 레시피를 검색
     *
     * @param title 검색할 제목 키워드 (필수)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 제목에 키워드가 포함된 레시피 목록 (페이징)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<RecipeListAdminDTO>> searchRecipesByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminRecipeService.searchRecipesByTitle(title, page, size));
    }
    /**
     * 특정 회원이 작성한 판매 거래글 리스트 조회
     * - 요청 경로: /admin/users/{userId}/sales
     * - 응답: 거래글 ID, 제목, 작성일, 거래 상태 (0=거래중, 1=거래완료)
     */
    @GetMapping("/users/{userId}/sales")
    public List<UserTradePostSimpleDTO> getUserSales(@PathVariable int userId) {
        return tradePostRepository.findSalesByUserId(userId);
    }
    /**
     * 특정 회원이 구매한 거래글 리스트 조회
     * - 요청 경로: /admin/users/{userId}/purchases
     * - 응답: 거래글 ID, 제목, 작성일, 거래 상태 (0=거래중, 1=거래완료)
     */
    @GetMapping("/users/{userId}/purchases")
    public List<UserTradePostSimpleDTO> getUserPurchases(@PathVariable int userId) {
        return tradePostRepository.findPurchasesByUserId(userId);
    }

    /**
     * ✅ 특정 회원의 포인트 적립 내역 조회
     * GET /admin/users/{userId}/points/earned
     */
    @GetMapping("/users/{userId}/points/earned")
    public List<PointHistoryDTO> getEarnedPoints(@PathVariable int userId) {
        return pointService.getEarnedHistory(userId).stream()
                .map(PointHistoryDTO::from)
                .toList();
    }

    /**
     * ✅ 특정 회원의 포인트 사용 내역 조회
     * GET /admin/users/{userId}/points/used
     */
    @GetMapping("/users/{userId}/points/used")
    public List<PointHistoryDTO> getUsedPoints(@PathVariable int userId) {
        return pointService.getUsedHistory(userId).stream()
                .map(PointHistoryDTO::from)
                .toList();
    }
    /**
     * [POST] /admin/users/{userId}/block
     * 관리자 회원 차단 API
     * - PathVariable: userId (차단할 회원 id)
     * - RequestBody: {"reason": "스팸 계정으로 확인되어 차단"}
     */
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable int userId,
            @RequestBody BlockRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails admin // 관리자의 username
    ) {
        adminService.blockUser(userId, admin.getUsername(), dto.getReason());
        return ResponseEntity.ok("차단 완료");
    }
    /**
     * [POST] /admin/users/{userId}/block
     * 관리자 회원 차단 해제 API
     * - PathVariable: userId (차단할 회원 id)
     * - RequestBody: {"reason": "생일기념 차단 해제"}
     */
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<?> unblockUser(
            @PathVariable int userId,
            @RequestBody BlockRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        adminService.unblockUser(userId, admin.getUsername(), dto.getReason());
        return ResponseEntity.ok("차단 해제 완료");
    }
    /**
     * 🔍 관리자 레시피 상세 조회 (리뷰 포함)
     * @param recipeId 레시피 ID
     * @return RecipeDetailAdminDTO
     * reviews는 List로 넘어감
     */
    @GetMapping("/recipes/{recipeId}")
    public ResponseEntity<RecipeDetailAdminDTO> getRecipeDetail(@PathVariable Long recipeId) {
        return ResponseEntity.ok(adminRecipeService.getRecipeDetail(recipeId));
    }

    /**
     * 특정 회원이 작성한 거래 후기 조회
     * - GET /admin/users/{userId}/reviews/written
     */
    @GetMapping("/users/{userId}/reviews/written")
    public List<TpReviewSimpleDTO> getWrittenTradeReviews(@PathVariable int userId) {
        return tpReviewRepository.findReviewsWrittenByUser(userId);
    }

    /**
     * 특정 회원이 받은 거래 후기 조회 (내 거래글에 남겨진)
     * - GET /admin/users/{userId}/reviews/received
     */
    @GetMapping("/users/{userId}/reviews/received")
    public List<TpReviewSimpleDTO> getReceivedTradeReviews(@PathVariable int userId) {
        return tpReviewRepository.findReviewsReceivedByUser(userId);
    }

    /**
     * [GET] /admin/users/blocked
     * 차단된 회원 리스트 조회 (페이징)
     * - 응답: 회원 id, 이름(name), 아이디(username), 차단일(blockedAt)
     */
    @GetMapping("/users/blocked")
    public Page<UserBlockedListDTO> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userRepository.findByBlockedTrue(
                PageRequest.of(page, size, Sort.by("blockedAt").descending())
        ).map(user -> new UserBlockedListDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBlockedAt()
        ));
    }

    /**
     * [GET] /admin/users/{userId}/block-reason
     * 차단된 회원의 최신 차단 사유/관리자/일시 조회
     * - PathVariable: userId
     * - 응답: 사유(reason), 차단관리자(blockedBy), 차단일(blockedAt)
     */
    @GetMapping("/users/{userId}/block-reason")
    public ResponseEntity<UserBlockReasonDTO> getBlockReason(@PathVariable int userId) {
        // 1. 유저 차단 여부 체크 (옵션)
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));
        if (!user.isBlocked()) {
            return ResponseEntity.badRequest().body(null);
        }
        // 2. 최신 차단 로그 조회
        List<AdminLog> logs = adminLogRepository.findRecentUserBlocks(userId);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AdminLog lastBlock = logs.get(0);
        UserBlockReasonDTO dto = new UserBlockReasonDTO(
                lastBlock.getReason(),
                lastBlock.getAdminUsername(),
                lastBlock.getCreatedAt()
        );
        return ResponseEntity.ok(dto);
    }
    /**
     * 통계 파라미터 사용
     *
     * @param type  통계 유형 (필수) - DAILY / MONTHLY / YEARLY
     * @param year  연도 (MONTHLY 또는 YEARLY일 때 필수)  예: 2024
     * @param month 월 (MONTHLY일 때 필수)            예: 5
     * @param start 시작일 (DAILY일 때 필수)           예: 2024-04-01
     * @param end   종료일 (DAILY일 때 필수)           예: 2024-04-30
     *
     * 📌 호출 예시:
     * - DAILY   : admin/stats/recipes?type=DAILY&start=2024-04-01&end=2024-04-30
     * - MONTHLY : admin/stats/recipes?type=MONTHLY&year=2024&month=5
     * - YEARLY  : admin/stats/recipes?type=YEARLY&year=2024
     */

    /**
     * [GET] /admin/stats/recipes
     * 레시피 작성 수 통계 API
     * - 통계 유형(type): DAILY, MONTHLY, YEARLY
     * - 필터: 시작일~종료일(start, end), 연도(year), 월(month)
     * - 응답: 날짜/월별 작성된 레시피 수
     */
    @GetMapping("/stats/recipes")
    public ResponseEntity<List<RecipeStatDTO>> getRecipeStats(
            @RequestParam("type") StatType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<RecipeStatDTO> stats = recipeStatService.getRecipeStats(type, start, end, year, month);
        return ResponseEntity.ok(stats);
    }
    /**
     * [GET] /admin/stats/recipes/likes
     * 레시피 찜 수 통계 API
     * - 통계 유형(type): DAILY, MONTHLY, YEARLY
     * - 필터: 시작일~종료일(start, end), 연도(year), 월(month)
     * - 응답: 날짜/월별 찜 수
     */
    @GetMapping("/stats/recipes/likes")
    public ResponseEntity<List<RecipeStatDTO>> getLikeStats(
            @RequestParam("type") StatType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<RecipeStatDTO> result;

        if (type == StatType.YEARLY && year != null) {
            result = likeRecipeRepository.countLikesByYear(year).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "월", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.MONTHLY && year != null && month != null) {
            result = likeRecipeRepository.countLikesByMonth(year, month).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "일", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.DAILY && start != null && end != null) {
            result = likeRecipeRepository.countLikesByDateRange(
                            start.atStartOfDay(), end.atTime(23, 59, 59))
                    .stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());

        } else {
            throw new IllegalArgumentException("요청 파라미터가 부족하거나 잘못되었습니다.");
        }

        return ResponseEntity.ok(result);
    }
    /**
     * [GET] /admin/stats/recipes/recommends
     * 레시피 추천 수 통계 API
     * - 통계 유형(type): DAILY, MONTHLY, YEARLY
     * - 필터: 시작일~종료일(start, end), 연도(year), 월(month)
     * - 응답: 날짜/월별 추천 수
     */
    @GetMapping("/stats/recipes/recommends")
    public ResponseEntity<List<RecipeStatDTO>> getRecommendStats(
            @RequestParam("type") StatType type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<RecipeStatDTO> result;

        if (type == StatType.YEARLY && year != null) {
            result = recommendRecipeRepository.countByYear(year).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "월", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.MONTHLY && year != null && month != null) {
            result = recommendRecipeRepository.countByMonth(year, month).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "일", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.DAILY && start != null && end != null) {
            result = recommendRecipeRepository.countByDateRange(
                            start.atStartOfDay(), end.atTime(23, 59, 59))
                    .stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());

        } else {
            throw new IllegalArgumentException("요청 파라미터가 부족하거나 잘못되었습니다.");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * [GET] /admin/stats/recipes/categories
     * 레시피 카테고리별 통계 조회 API
     * - 응답: 한식, 중식, 양식 등 카테고리별 레시피 수
     */
    @GetMapping("/stats/recipes/categories")
    public ResponseEntity<List<RecipeStatDTO>> getCategoryStats(
            @RequestParam(required = false) String category) {
        log.debug("카테고리 파라미터: {}", category);

        if (category != null && !category.equals("전체")) {
            return ResponseEntity.ok(recipeStatService.getMonthlyCategoryStatsByName(category));
        } else {
            return ResponseEntity.ok(recipeStatService.getCategoryStats());
        }
    }

    /**
     * [GET] /api/admin/reports/boards
     * 게시글 신고 리스트 조회 API (페이징)
     * @return 최신순 정렬된 게시글 신고 리스트
     */
    @GetMapping("/reports/boards")
    public Page<ReportResponseDTO> getBoardReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reportService.getBoardReports(pageable);
    }

    /**
     * [GET] /api/admin/reports/comments
     * 댓글 신고 리스트 조회 API (페이징)
     * @return 최신순 정렬된 댓글 신고 리스트
     */
    @GetMapping("/reports/comments")
    public Page<ReportResponseDTO> getCommentReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reportService.getCommentReports(pageable);
    }

    /**
     * [GET] /api/admin/reports/boards/search
     * 게시글 신고 검색 API
     *
     * @param keyword 검색 키워드 (필수)
     * @return 키워드가 포함된 게시글 신고 리스트 (최신순)
     * 예시: /api/admin/reports/boards/search?keyword=욕설&page=0&size=10
     */
    @GetMapping("/reports/boards/search")
    public Page<ReportResponseDTO> searchBoardReports(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reportService.searchBoardReports(keyword, pageable);
    }

    /**
     * [GET] /api/admin/reports/comments/search
     * 댓글 신고 검색 API
     *
     * @param keyword 검색 키워드 (필수)
     * @return 키워드가 포함된 댓글 신고 리스트 (최신순)
     * 예시: /api/admin/reports/comments/search?keyword=비속어&page=0&size=10
     */
    @GetMapping("/reports/comments/search")
    public Page<ReportResponseDTO> searchCommentReports(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reportService.searchCommentReports(keyword, pageable);
    }

    /**
     * [GET] /admin/boards/logs/deleted
     * 삭제된 게시글 로그 리스트 조회 (페이징)
     * - 응답: 삭제한 관리자 아이디(adminUsername), 작업(action), 대상 타입(targetType),
     *         대상 ID(targetId), 상세 사유(reason), 삭제 일시(createdAt)
     * - 예시: /admin/boards/logs/deleted?page=0&size=10
     */
    @GetMapping("/boards/logs/deleted")
    public Page<DeletedLogDTO> getDeletedLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return adminLogRepository.findDeletedLogs(pageable)
                .map(log -> new DeletedLogDTO(
                        log.getAdminUsername(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getReason(),
                        log.getCreatedAt()
                ));
    }
    /**
     * [GET] /admin/comments/logs/deleted
     * 삭제된 댓글 로그 리스트 조회 (페이징)
     * - 응답: 삭제한 관리자(adminUsername), 작업(action), 대상 타입(targetType),
     *         대상 ID(targetId), 삭제 사유(reason), 삭제 일시(createdAt)
     * - 예시: /admin/comments/logs/deleted?page=0&size=10
     */
    @GetMapping("/comments/logs/deleted")
    public Page<DeletedLogDTO> getDeletedCommentLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return adminLogRepository.findDeletedCommentLogs(pageable)
                .map(log -> new DeletedLogDTO(
                        log.getAdminUsername(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getReason(),
                        log.getCreatedAt()
                ));
    }

    /**
     * [GET] /admin/recipes/logs/deleted
     * 삭제된 레시피 로그 리스트 조회 (페이징)
     * - 응답: 삭제한 관리자(adminUsername), 작업(action), 대상 타입(targetType),
     *         대상 ID(targetId), 삭제 사유(reason), 삭제 일시(createdAt)
     * - 예시: /admin/recipes/logs/deleted?page=0&size=10
     */
    @GetMapping("/recipes/logs/deleted")
    public Page<DeletedLogDTO> getDeletedRecipeLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return adminLogRepository.findDeletedRecipeLogs(pageable)
                .map(log -> new DeletedLogDTO(
                        log.getAdminUsername(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getReason(),
                        log.getCreatedAt()
                ));
    }
    /**
     * [DELETE] /admin/recipes/reviews/{reviewId}
     * 레시피 리뷰 삭제
     * - 관리자 로그 기록 포함
     */
    @DeleteMapping("/recipes/reviews/{reviewId}")
    public ResponseEntity<Void> deleteRecipeReviewByAdmin(
            @PathVariable Long reviewId,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        reviewService.deleteReviewByAdmin(reviewId, admin.getUsername(), reason);
        return ResponseEntity.ok().build();
    }

    /**
     * [GET] /admin/recipes/reviews/logs/deleted
     * 삭제된 레시피 리뷰 로그 리스트 조회 (페이징)
     * - 응답: 삭제한 관리자(adminUsername), 작업(action), 대상 타입(targetType),
     *         대상 ID(targetId), 삭제 사유(reason), 삭제 일시(createdAt)
     */
    @GetMapping("/recipes/reviews/logs/deleted")
    public Page<DeletedLogDTO> getDeletedRecipeReviewLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return adminLogRepository.findDeletedRecipeReviewLogs(pageable)
                .map(log -> new DeletedLogDTO(
                        log.getAdminUsername(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getReason(),
                        log.getCreatedAt()
                ));
    }
    @PostMapping("/ingredients")
    public ResponseEntity<IngredientMasterResponse> createIngredient(
            @RequestBody IngredientAdminRequest dto,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ResponseEntity.ok(adminIngredientService.create(admin.getUsername(), dto));
    }

    @PutMapping("/ingredients/{id}")
    public ResponseEntity<IngredientMasterResponse> updateIngredient(
            @PathVariable Long id,
            @RequestBody IngredientAdminRequest dto,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        return ResponseEntity.ok(adminIngredientService.update(id, admin.getUsername(), dto));
    }

    @DeleteMapping("/ingredients/{id}")
    public ResponseEntity<String> deleteIngredient(
            @PathVariable Long id,
            @RequestBody DeleteRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        adminIngredientService.delete(id, admin.getUsername(), requestDTO.getReason());
        return ResponseEntity.ok("재료 삭제 및 로그 기록 완료");
    }

    @GetMapping("/ingredients/{id}")
    public ResponseEntity<IngredientMasterResponse> getIngredient(@PathVariable Long id) {
        return ResponseEntity.ok(adminIngredientService.getById(id));
    }

    @GetMapping("/units")
    public ResponseEntity<List<UnitEntity>> getAllUnits() {
        return ResponseEntity.ok(unitRepository.findAll());
    }

    @GetMapping("/ingredients/categories")
    public ResponseEntity<List<String>> getIngredientCategories() {
        List<String> categories = Arrays.stream(IngredientCategory.values())
                .map(Enum::name) // enum 이름 그대로
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }
}
