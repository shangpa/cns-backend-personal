package com.example.springjwt.pantry.history;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.pantry.dto.PantryHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pantries/{pantryId}/history")
@RequiredArgsConstructor
public class PantryHistoryController {

    private final PantryHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<PantryHistoryDto>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long pantryId,
            @RequestParam(value = "ingredientName", required = false) String ingredientName
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        return ResponseEntity.ok(historyService.list(user, pantryId, ingredientName));
    }
}
