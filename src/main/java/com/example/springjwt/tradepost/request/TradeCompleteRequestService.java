package com.example.springjwt.tradepost.request;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.notification.NotificationRequestDTO;
import com.example.springjwt.notification.NotificationService;
import com.example.springjwt.tradepost.TradePost;
import com.example.springjwt.tradepost.TradePostRepository;
import com.example.springjwt.tradepost.TradePostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeCompleteRequestService {

    private final TradePostRepository tradePostRepository;
    private final UserRepository userRepository;
    private final TradeCompleteRequestRepository requestRepository;
    private final NotificationService notificationService;

    @Transactional
    public ResponseEntity<String> createRequest(Long postId, String username) {
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글이 존재하지 않습니다."));
        UserEntity user = userRepository.findByUsername(username);
        UserEntity seller = post.getUser(); // 판매자 가져오기 ← 반드시 있어야 함!

        if (requestRepository.existsByTradePostAndRequester(post, user)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("이미 요청한 사용자입니다");
        }

        if (user.getPoint() < post.getPrice()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("포인트가 부족합니다");
        }

        TradeCompleteRequest request = TradeCompleteRequest.of(post, user);
        requestRepository.save(request);
        // ✅ 알림 전송
        String content = user.getUsername() + "님이 '" + post.getTitle() + "' 거래글에 거래 완료 요청을 보냈습니다.";
        NotificationRequestDTO notification = new NotificationRequestDTO();
        notification.setUserId(seller.getId());
        notification.setCategory("TRADE_REQUEST");
        notification.setContent(content);
        notificationService.notifyUser(notification);
        return ResponseEntity.ok("거래 요청 완료");
    }

    public List<UserEntity> getRequesters(Long postId) {
        List<TradeCompleteRequest> requests = requestRepository.findByTradePost_TradePostId(postId);
        return requests.stream()
                .map(TradeCompleteRequest::getRequester)
                .collect(Collectors.toList());
    }
}
