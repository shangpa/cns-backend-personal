package com.example.springjwt.tradepost.up;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.point.PointService;
import com.example.springjwt.tradepost.TradePost;
import com.example.springjwt.tradepost.TradePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradePostUpService {

    private final TradePostRepository tradePostRepository;
    private final UpHistoryRepository upHistoryRepository;
    private final PointService pointService;

    public static final int UP_COST = 500;

    @Transactional
    public TradePostUpResult up(Long postId, UserEntity user) {
        TradePost post = tradePostRepository.findWithOwner(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 소유자 검증 (int ↔ int)
        if (post.getUser().getId() != user.getId()) {
            throw new SecurityException("본인 게시글만 UP할 수 있습니다.");
        }

        // 상태 검증
        if (post.isUpProhibited()) {
            throw new IllegalStateException("거래완료된 게시글은 UP할 수 없습니다.");
        }

        // 1) 포인트 차감
        pointService.usePoint(user, UP_COST, "TRADE_UP::postId=" + postId);

        // 2) 타임스탬프 갱신
        LocalDateTime now = LocalDateTime.now();
        post.markUpped(now);

        // 3) 이력 저장(감사/통계)
        upHistoryRepository.save(UpHistory.builder()
                .postId(postId)
                .userId(user.getId())
                .usedPoints(UP_COST)
                .reason("PAID")
                .createdAt(now)
                .build());

        return new TradePostUpResult(post.getTradePostId(), UP_COST, "PAID", now);
    }
}
