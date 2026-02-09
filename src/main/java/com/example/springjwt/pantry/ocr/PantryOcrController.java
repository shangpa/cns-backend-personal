package com.example.springjwt.pantry.ocr;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pantries/{pantryId}/ocr")
@RequiredArgsConstructor
public class PantryOcrController {

    private final PantryOcrService service;
    private final UserRepository userRepo;

    @PostMapping("/confirm")
    public List<OcrConfirmResult> confirm(
            @AuthenticationPrincipal(expression = "username") String username,
            @PathVariable Long pantryId,
            @RequestBody @Valid OcrConfirmRequest req
    ) {
        UserEntity user = userRepo.findOptionalByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        return service.confirm(user, pantryId, req);
    }
}
