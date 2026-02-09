package com.example.springjwt.shorts;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.board.CommentRequestDTO;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.notification.FCMService;
import com.example.springjwt.shorts.comment.CommentResponseDTO;
import com.example.springjwt.shorts.comment.ShortComment;
import com.example.springjwt.shorts.comment.ShortCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController
@RequestMapping("/api/shorts")
@RequiredArgsConstructor
public class ShortsVideoController {

    private final ShortsVideoService shortsVideoService;
    private final ShortsVideoRepository shortsVideoRepository;
    private final UserRepository userRepository;
    private final ShortCommentRepository shortCommentRepository;
    private final FCMService fcmService;
   /* // 파일만 업로드
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadShortsFileOnly(
            @RequestParam("video") MultipartFile video,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            // 파일만 저장하고 DB 등록은 안 함
            String uploadDir = "uploads/shorts/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = UUID.randomUUID() + "_" + video.getOriginalFilename();
            File saveFile = new File(uploadDir, fileName);
            video.transferTo(saveFile);

            // 프론트에서 미리보기 할 수 있게 URL만 리턴
            String videoUrl = "/uploads/shorts/" + fileName;
            return ResponseEntity.ok(videoUrl);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }*/

    // 최종 등록 (로그인 필요)
    @PostMapping("/register")
    public ResponseEntity<Long> registerShorts(
            @RequestBody RecipeShortCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).build();
            }
            if (request.getTitle() == null || request.getTitle().isBlank()
                    || request.getVideoUrl() == null || request.getVideoUrl().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            UserEntity user = userDetails.getUserEntity();

            // (선택) 서비스 내부에서 절대→상대 변환하도록 통일
            ShortsVideo saved = shortsVideoService.createShorts(
                    request.getTitle(),
                    request.getVideoUrl(),
                    request.getThumbnailUrl(),
                    request.isPublic(),
                    user
            );
            return ResponseEntity.ok(saved.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 최신 숏츠
    @GetMapping("/latest")
    public ResponseEntity<List<ShortsVideo>> getLatest() {
        return ResponseEntity.ok(shortsVideoService.getLatestShorts());
    }

    // 인기 숏츠
    @GetMapping("/popular")
    public ResponseEntity<List<ShortsVideo>> getPopular() {
        return ResponseEntity.ok(shortsVideoService.getPopularShorts());
    }

    // 조회수 증가
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> increaseView(@PathVariable Long id) {
        shortsVideoService.incrementViewCount(id); // 이름만 살짝 다르게
        return ResponseEntity.ok().build();
    }
    /*
    랜덤재생
    @GetMapping("/random")
    public ResponseEntity<List<ShortsListDto>> randomSimple(
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(shortsVideoService.getRandomSimple(size));
    }*/


    // 랜덤시드 재생 (비로그인 허용)
    @GetMapping("/random")
    public ResponseEntity<List<ShortsListDto>> randomBySeed(
            @RequestParam(required = false) String seed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails // null 가능
    ) {
        // seed 기본값
        String s = (seed == null || seed.isBlank()) ? "default" : seed;

        // 요청 방어
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        var list = shortsVideoService.getRandomBySeed(s, safePage, safeSize);
        return ResponseEntity.ok(list);
    }

    // 유저별 정렬 (비로그인 허용)
    @GetMapping("/{userId}")
    public ResponseEntity<ShortsUserVideoListResponse> getUserShorts(
            @PathVariable int userId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Sort sortObj = switch (sort) {
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "date"  -> Sort.by(Sort.Direction.ASC,  "createdAt");
            default      -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), sortObj);
        var pageResult = shortsVideoRepository.findByUser_IdAndIsPublicTrue(userId, pageable);

        var list = pageResult.getContent().stream()
                .map(ShortsUserVideoDto::from)
                .toList();

        return ResponseEntity.ok(new ShortsUserVideoListResponse(list));
    }

    // 랜덤 3개(레시피 탭) (비로그인 허용)
    @GetMapping("/random3")
    public ResponseEntity<List<ShortsCardDto>> random3() {
        return ResponseEntity.ok(shortsVideoService.getRandom3Cards());
    }

    //댓글 작성
    @PostMapping("/{id}/comment")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestBody CommentRequestDTO dto,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userRepository.findByUsername(userDetails.getUsername());
        Long shortsId = Long.valueOf(id); // id가 int일 경우 변환
        ShortsVideo shorts = shortsVideoRepository.findById(shortsId)
                .orElseThrow(() -> new RuntimeException("ShortsVideo not found"));

        ShortComment comment = new ShortComment();
        comment.setUser(user);
        comment.setShortsVideo(shorts);
        comment.setContent(dto.getContent());

        shortCommentRepository.save(comment);
        shorts.setCommentCount(shorts.getCommentCount() + 1);
        UserEntity writer = shorts.getUser();
        if (!writer.getUsername().equals(user.getUsername())) {
            fcmService.sendNotificationToUser(
                    writer,
                    "댓글 알림",
                    user.getUsername() + "님이 당신의 게시글에 댓글을 남겼습니다.",
                    "COMMUNITY"
            );
        }
        shortsVideoRepository.save(shorts);
        return ResponseEntity.ok("댓글 등록 완료");
    }

    //댓글 조회
    @GetMapping("/{id}/comments")
    public List<CommentResponseDTO> getComments(@PathVariable Long id) {
        return shortCommentRepository.findByShortsVideoIdOrderByCreatedAtAsc(id).stream()
                .map(CommentResponseDTO::fromEntity)
                .toList();
    }

    //검색
    @GetMapping("/search")
    public List<ShortsSearchItem> search(@RequestParam String keyword) {
        return shortsVideoService.search(keyword);
    }
}
