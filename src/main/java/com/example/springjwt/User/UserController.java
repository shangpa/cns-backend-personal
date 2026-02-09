package com.example.springjwt.User;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.dto.LoginInfoResponse;
import com.example.springjwt.dto.UserUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    // 사용자 위치 저장
    @PostMapping("/location")
    public ResponseEntity<?> setUserLocation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        int userId = userDetails.getUserEntity().getId();
        userService.updateUserLocation(userId, latitude, longitude);
        return ResponseEntity.ok("위치 저장 완료");
    }

    // 사용자 위치 조회
    @GetMapping("/location")
    public ResponseEntity<?> getUserLocation(@AuthenticationPrincipal CustomUserDetails userDetails) {
        int userId = userDetails.getUserEntity().getId();
        UserEntity user = userService.getUserById(userId);

        return ResponseEntity.ok(new UserLocationResponse(user.getLatitude(), user.getLongitude()));
    }

    // DTO 클래스 (내부 클래스 or 별도 파일로 분리 가능)
    record UserLocationResponse(Double latitude, Double longitude) {}

    // 마이페이지 사용자 이름 출력
    @GetMapping("/profile")
    public ResponseEntity<LoginInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserEntity u = userDetails.getUserEntity();
        return ResponseEntity.ok(new LoginInfoResponse(
                (long) u.getId(),
                u.getUsername(),
                u.getName(),
                u.getProfileImageUrl()
        ));
    }


    // 마이페이지 개인정보수정
    @PutMapping("/update")
    public ResponseEntity<?> updateUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequestDTO dto) {

        userService.updateUser(userDetails.getUserEntity().getId(), dto);
        return ResponseEntity.ok("수정 완료");
    }
    
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("image") MultipartFile image
    ) {
        int userId = userDetails.getUserEntity().getId();

        // 1) 파일 저장
        String url = userService.saveProfileImage(userId, image);

        // 2) DB 업데이트
        userService.updateProfileImageUrl(userId, url);

        // 3) 클라이언트에 반환
        return ResponseEntity.ok(Map.of("profileImageUrl", url));
    }


    @PostMapping("/check-password")
    public ResponseEntity<?> checkPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        String inputPassword = body.get("password");
        String storedPassword = userDetails.getPassword();

        boolean matches = passwordEncoder.matches(inputPassword, storedPassword);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/id")
    public ResponseEntity<Long> getUserIdByUsername(@RequestParam String username) {
        UserEntity user = userRepository.findByUsername(username);
        return ResponseEntity.ok((long) user.getId());
    }
    //id로 이름찾기
    @GetMapping("/profile-by-id")
    public ResponseEntity<Map> getUsernameById(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long id
    ) {
        // (Optional) 인증된 사용자 확인
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        UserEntity user = userRepository.findById(id.intValue()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of("username", user.getUsername()));
    }
}
