package com.example.springjwt.point;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point")
public class PointController {

    private final PointService pointService;
    private final UserRepository userRepository;

    //포인트 적립 (테스트용)
    @PostMapping("/add")
    public ResponseEntity<String> addPoint(
            @RequestParam int userId,
            @RequestParam PointActionType action,
            @RequestParam(defaultValue = "1") int count,
            @RequestParam String description
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        pointService.addPoint(user, action, count, description);
        return ResponseEntity.ok("포인트가 적립되었습니다.");
    }

    // 포인트 차감 (테스트용)
    @PostMapping("/use")
    public ResponseEntity<String> usePoint(
            @RequestParam int userId,
            @RequestParam int amount,
            @RequestParam String description
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        pointService.usePoint(user, amount, description);
        return ResponseEntity.ok("포인트가 차감되었습니다.");
    }

    //유저 포인트 이력 조회
    @GetMapping("/user/{id}/history")
    public ResponseEntity<List<PointHistory>> getPointHistory(@PathVariable int id) {
        List<PointHistory> history = pointService.getHistory(id);
        return ResponseEntity.ok(history);
    }
    //남은 포인트 조회
    @GetMapping("/my-point")
    public ResponseEntity<Integer> getMyPoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String username = userDetails.getUsername();
        System.out.println("로그인한 유저네임 = " + username);

        UserEntity realTimeUser = userRepository.findByUsername(username);

        if (realTimeUser == null) {
            throw new IllegalArgumentException("유저를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(realTimeUser.getPoint());
    }

    //포인트 사용내역
    @GetMapping("/my-history")
    public ResponseEntity<List<PointHistoryResponseDTO>> getMyHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String username = userDetails.getUsername(); // 로그인한 유저의 ID 또는 username
        UserEntity user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("유저를 찾을 수 없습니다.");
        }

        List<PointHistory> historyList = pointService.getHistory(user.getId());

        List<PointHistoryResponseDTO> result = historyList.stream().map(h -> new PointHistoryResponseDTO(
                h.getAction() != null ? h.getAction().name() : "USE",
                h.getPointChange(),
                h.getDescription(),
                h.getCreatedAt()
        )).toList();

        return ResponseEntity.ok(result);
    }
}
