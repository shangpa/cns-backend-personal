package com.example.cns.image;

import com.example.cns.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
public class ImageUploadController {

    private final JWTUtil jwtUtil; // JWT 인증 유틸

    private static final String UPLOAD_DIR = "uploads/";

    public ImageUploadController(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestHeader("Authorization") String token, // 🔹 JWT 토큰 인증 추가
            @RequestParam("image") MultipartFile file
    ) {
        // 🔹 1️⃣ 토큰 검증 (Bearer 제거 후 토큰만 추출)
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 필요합니다.");
        }
        String jwtToken = token.substring(7);
        if (jwtUtil.isExpired(jwtToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
        }

        // 🔹 2️⃣ 토큰에서 사용자 정보 추출 (로그 기록)
        String username = jwtUtil.getUsername(jwtToken);
        log.debug("이미지 업로드 요청 - 사용자: {}", username);

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미지 파일이 비어 있습니다.");
        }

        try {
            // 업로드 폴더 생성
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성 (UUID + 원본 확장자)
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath);

            // 저장된 파일의 URL 반환
            String fileUrl = "/uploads/" + newFileName;
            return ResponseEntity.ok().body(fileUrl);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패: " + e.getMessage());
        }
    }
}