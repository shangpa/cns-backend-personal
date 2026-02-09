package com.example.springjwt.auth;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserService;
import com.example.springjwt.jwt.JWTUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JWTUtil jwtUtil; // ✅ 기존 JwtProvider 대신 JWTUtil 사용

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        System.out.println("🔍 클라이언트에서 받은 ID 토큰: " + idToken);

        GoogleIdToken.Payload payload = GoogleTokenVerifierUtil.verify(idToken);

        if (payload == null) {
            System.out.println("❌ ID Token 검증 실패 - payload null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        System.out.println("✅ ID Token payload 검증 성공 - email: " + email + ", name: " + name);

        UserEntity user = userService.findOrCreateGoogleUser(email, name);
        String token = jwtUtil.createJwt(user.getUsername(), user.getRole(), 1000L * 60 * 60 * 10);

        return ResponseEntity.ok(new LoginResponse(token, user.getId(), "Google Login successful"));
    }
}
