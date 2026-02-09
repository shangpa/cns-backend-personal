package com.example.springjwt.fridge.history;

import com.example.springjwt.User.UserRepository;
import com.example.springjwt.fridge.history.FridgeHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fridge-history")
@RequiredArgsConstructor
public class FridgeHistoryController {

    private final FridgeHistoryService historyService;
    private final UserRepository userRepository;

    //특정 재료명에 대한 이력 조회
    @GetMapping
    public ResponseEntity<List<FridgeHistoryResponse>> getHistoryByIngredient(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String ingredientName
    ) {
        Long userId = (long) userRepository.findOptionalByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."))
                .getId();

        List<FridgeHistoryResponse> history = historyService.getHistory(userId, ingredientName);
        return ResponseEntity.ok(history);
    }

    //로그인한 사용자의 모든 재료 이력 조회
    @GetMapping("/all")
    public ResponseEntity<List<FridgeHistoryResponse>> getAllHistoryByUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = (long) userRepository.findOptionalByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."))
                .getId();
        if (userDetails == null) {
            throw new RuntimeException("인증 실패: userDetails가 null입니다");
        }
        List<FridgeHistoryResponse> historyList = historyService.getAllHistoryByUser(userId);
        return ResponseEntity.ok(historyList);
    }

}
