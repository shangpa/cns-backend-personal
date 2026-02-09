package com.example.springjwt.image;

import com.example.springjwt.jwt.JWTUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class VideoUploadController {
    private final JWTUtil jwtUtil;
    private static final String UPLOAD_DIR = "uploads/videos/";

    public VideoUploadController(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload-video")
    public ResponseEntity<?> uploadVideo(
            @RequestHeader("Authorization") String token,
            @RequestParam("video") MultipartFile file
    ) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 필요합니다.");
        }

        String jwtToken = token.substring(7);
        if (jwtUtil.isExpired(jwtToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
        }

        String username = jwtUtil.getUsername(jwtToken);
        System.out.println("🎥 동영상 업로드 요청 - 사용자: " + username);

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비디오 파일이 비어 있습니다.");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.copy(file.getInputStream(), filePath);
            String fileUrl = "/uploads/videos/" + newFileName;

            // 실제 저장 경로 로그 출력
            System.out.println("📝 저장된 파일 경로: " + filePath.toAbsolutePath());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(fileUrl);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("비디오 업로드 실패: " + e.getMessage());
        }
    }
}
