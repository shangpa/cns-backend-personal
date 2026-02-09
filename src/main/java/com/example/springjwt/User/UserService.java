package com.example.springjwt.User;

import com.example.springjwt.dto.UserUpdateRequestDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadRoot;

    @Transactional
    public void updateUserLocation(int id, Double latitude, Double longitude) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setLatitude(latitude);
        user.setLongitude(longitude);
    }

    public UserEntity getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    public UserEntity getUser(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void updateUser(int userId, UserUpdateRequestDTO dto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        // @Transactional이라 save() 생략 가능
    }

    public UserEntity findOrCreateGoogleUser(String email, String name) {
        UserEntity user = userRepository.findByUsername(email);
        if (user == null) {
            user = new UserEntity();
            user.setUsername(email);
            user.setName(name);
            user.setPassword("");
            user.setRole("ROLE_USER");
            user = userRepository.save(user);
        }
        return user;
    }

    public String saveProfileImage(int userId, MultipartFile image) {
        try {
            String original = image.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.') + 1)
                    : "jpg";

            String filename = "user_" + userId + "_" + System.currentTimeMillis() + "." + ext;

            Path baseDir = Paths.get(uploadRoot, "profile").toAbsolutePath().normalize();
            Files.createDirectories(baseDir);                  // ← 폴더 자동 생성
            Path target = baseDir.resolve(filename);

            image.transferTo(target.toFile());

            // http://localhost:8080/uploads/profile/파일.jpg
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/profile/")
                    .path(filename)
                    .toUriString();
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장 실패", e);
        }
    }

    @Transactional
    public void updateProfileImageUrl(int userId, String url) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setProfileImageUrl(url);
    }
}
