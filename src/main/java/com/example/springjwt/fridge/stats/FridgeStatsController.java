package com.example.springjwt.fridge.stats;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

// REST API 컨트롤러임을 명시
@RestController
// 공통 URL prefix 지정 -> /api/fridges/stats 로 시작하는 요청을 처리
@RequestMapping("/api/fridges/stats")
public class FridgeStatsController {

    // 냉장고 통계 비즈니스 로직을 수행하는 서비스
    private final FridgeStatsService statsService;
    // 사용자 정보를 DB에서 조회하기 위한 Repository
    private final UserRepository userRepository;

    // 생성자 주입: Spring이 자동으로 Bean을 넣어줌
    public FridgeStatsController(FridgeStatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    /**
     * 로그인한 사용자의 냉장고 통계 조회 API
     * GET /api/fridges/stats/my
     *
     * @param userDetails Spring Security에서 주입해주는 로그인 사용자 정보
     * @return 냉장고 통계 DTO(FridgeStatsResponse)
     */
    @GetMapping("/my")
    public ResponseEntity<FridgeStatsResponse> getMyStats(@AuthenticationPrincipal UserDetails userDetails) {
        // 로그인하지 않은 경우 -> 401 Unauthorized 반환
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 현재 로그인한 사용자의 username 으로 DB에서 UserEntity 조회
        UserEntity user = userRepository.findOptionalByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // Service 계층 호출 -> 사용자별 냉장고 통계 데이터 생성
        FridgeStatsResponse stats = statsService.getStats(user);

        // 200 OK + 통계 데이터 반환
        return ResponseEntity.ok(stats);
    }
}
