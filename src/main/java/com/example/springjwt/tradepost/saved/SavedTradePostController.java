package com.example.springjwt.tradepost.saved;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.tradepost.TradePost;
import com.example.springjwt.tradepost.TradePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tradeposts")
public class SavedTradePostController {

    private final SavedTradePostRepository savedTradePostRepository;
    private final TradePostRepository tradePostRepository;
    private final UserRepository userRepository;

    @PostMapping("/{postId}/save-toggle")
    public ResponseEntity<String> toggleSaveTradePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글을 찾을 수 없습니다."));

        // 이미 저장된 상태이면 삭제
        savedTradePostRepository.findByUserAndTradePost(user, post).ifPresentOrElse(
                saved -> {
                    savedTradePostRepository.delete(saved);
                },
                () -> {
                    SavedTradePost newSave = new SavedTradePost(null, user, post, LocalDateTime.now());
                    savedTradePostRepository.save(newSave);
                }
        );

        return ResponseEntity.ok("찜 토글 완료");
    }

    @GetMapping("/saved")
    public ResponseEntity<List<Long>> getSavedTradePosts(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);

        List<Long> savedIds = savedTradePostRepository.findByUser(user)
                .stream()
                .map(saved -> saved.getTradePost().getTradePostId())
                .collect(Collectors.toList());

        return ResponseEntity.ok(savedIds);
    }

    @GetMapping("/{postId}/saved")
    public ResponseEntity<Boolean> isTradePostSaved(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글을 찾을 수 없습니다."));

        boolean saved = savedTradePostRepository.findByUserAndTradePost(user, post).isPresent();
        return ResponseEntity.ok(saved);
    }
}
