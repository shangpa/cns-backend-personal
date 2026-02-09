package com.example.springjwt.shorts;

import com.example.springjwt.User.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortsVideoService {

    private final ShortsVideoRepository shortsVideoRepository;

    public String storeFile(MultipartFile file) throws IOException {
        String uploadDir = "uploads/shorts/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String original = file.getOriginalFilename();
        String safeName = (original == null ? "video.mp4" : original).replaceAll("\\s+", "_");
        String fileName = UUID.randomUUID() + "_" + safeName;

        File saveFile = new File(uploadDir, fileName);
        file.transferTo(saveFile);
        return "/uploads/shorts/" + fileName;
    }

    private String toRelative(String url) {
        if (url == null || url.isBlank()) return null;
        int idx = url.indexOf("/uploads/");
        return (idx >= 0) ? url.substring(idx) : url; // "/uploads/..." 만 남김
    }

    public ShortsVideo createShorts(String title, String videoUrl, String thumbnailUrl, boolean isPublic, UserEntity user) {
        String relVideo = toRelative(videoUrl);
        String relThumb = toRelative(thumbnailUrl);
        ShortsVideo shortsVideo = ShortsVideo.builder()
                .title(title)
                .videoUrl(relVideo)
                .thumbnailUrl(relThumb)
                .isPublic(isPublic)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        return shortsVideoRepository.save(shortsVideo);
    }

    // ===== 아래 기존 메서드 유지 =====
    public ShortsVideo uploadVideo(MultipartFile file, String title, boolean isPublic, UserEntity user) throws IOException {
        String videoUrl = storeFile(file);
        return createShorts(title, videoUrl, /* thumbnailUrl */ null, isPublic, user); // null 추가
    }

    // 최신순
    public List<ShortsVideo> getLatestShorts() {
        return shortsVideoRepository.findTop10ByIsPublicTrueOrderByCreatedAtDesc();
    }

    // 인기순
    public List<ShortsVideo> getPopularShorts() {
        return shortsVideoRepository.findTop10ByIsPublicTrueOrderByViewCountDesc();
    }

    //랜덤조회
    public List<ShortsListDto> getRandomSimple(int size) {
        int limit = Math.max(1, size);
        return shortsVideoRepository.findRandomSimple(limit).stream().map(ShortsListDto::from).toList();
    }

    // 조회수 증가
    @Transactional
    public void incrementViewCount(Long shortsId) {
        int updated = shortsVideoRepository.incrementViewCount(shortsId);
        if (updated == 0) {
            throw new RuntimeException("숏츠 없음");
        }
    }

    //랜덤시드
    public List<ShortsListDto> getRandomBySeed(String seed, int page, int size) {
        int limit  = Math.max(1, size);          // size 0 방지
        int offset = Math.max(0, page) * limit;  // page 0 → 0, page 1 → limit

        String s = (seed == null || seed.isBlank()) ? "default" : seed;

        var rows = shortsVideoRepository.findRandomBySeedPositional(s, offset, limit);

        System.out.println("[randomBySeed] seed=" + s +
                ", page=" + page + ", size=" + size +
                ", offset=" + offset + ", fetched=" + rows.size());

        return rows.stream().map(ShortsListDto::from).toList();
    }

    //랜덤3개(레시피 탭)
    public List<ShortsCardDto> getRandom3Cards() {
        // 공개 숏츠 중 무작위 3개
        return shortsVideoRepository.findRandomSimple(3)
                .stream().map(ShortsCardDto::from).toList();
    }

    //검색
    public List<ShortsSearchItem> search(String keyword) {
        String q = (keyword == null) ? "" : keyword.trim();
        var list = shortsVideoRepository.searchPublicByKeyword(q);
        return list.stream()
                .map(s -> new ShortsSearchItem(
                        s.getId(),
                        s.getTitle(),
                        // 작성자명: 프로젝트 필드에 맞게 선택
                        (s.getUser() != null ? s.getUser().getUsername() : null),
                        s.getThumbnailUrl()
                ))
                .toList();
    }
}
