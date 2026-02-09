package com.example.springjwt.pantry.use;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pantries")
@RequiredArgsConstructor
public class PantryUseController {

    private final PantryUseService service;

    // 기본 팬트리에서 일괄 차감
    @PostMapping("/use")
    public ResponseEntity<Void> useInDefaultPantry(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<UseIngredientRequest> items
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        service.useFromDefaultPantry(user, items);
        return ResponseEntity.noContent().build(); // 204
    }
}
