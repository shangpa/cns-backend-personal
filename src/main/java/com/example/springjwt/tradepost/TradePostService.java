package com.example.springjwt.tradepost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.admin.dto.BoardMonthlyStatsDTO;
import com.example.springjwt.admin.dto.TradePostDetailResponseDTO;
import com.example.springjwt.admin.dto.TradePostListResponseDTO;
import com.example.springjwt.admin.log.AdminLogService;
import com.example.springjwt.chat.ChatMessageRepository;
import com.example.springjwt.chat.ChatRoom;
import com.example.springjwt.chat.ChatRoomRepository;
import com.example.springjwt.point.PointActionType;
import com.example.springjwt.point.PointService;
import com.example.springjwt.review.TradePost.TpReviewRepository;
import com.example.springjwt.tradepost.saved.SavedTradePostRepository;
import com.example.springjwt.util.DistanceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradePostService {

    private final TradePostRepository tradePostRepository;
    private final UserRepository userRepository;
    private final SavedTradePostRepository savedTradePostRepository;
    private final AdminLogService adminLogService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TpReviewRepository tpReviewRepository;
    private final PointService pointService;

    /* --------------------------- 생성/조회 기본 --------------------------- */

    public TradePost create(TradePostDTO dto, String username) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        TradePost tradePost = dto.toEntity();
        tradePost.setUser(user);
        tradePost.setStatus(TradePost.STATUS_ONGOING);

        // location: "lat,lng" 문자열에서 위경도 추출 (선택)
        String location = dto.getLocation();
        if (location != null && location.contains(",")) {
            try {
                String[] parts = location.split(",");
                double latitude = Double.parseDouble(parts[0].trim());
                double longitude = Double.parseDouble(parts[1].trim());
                tradePost.setLatitude(latitude);
                tradePost.setLongitude(longitude);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("위치 형식이 잘못되었습니다: " + location);
            }
        }
        return tradePostRepository.save(tradePost);
    }

    public TradePostDTO getTradePostById(Long id, String username) {
        TradePost tradePost = tradePostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래글이 존재하지 않습니다. ID=" + id));

        if (username != null) {
            UserEntity user = userRepository.findByUsername(username);
            if (user != null && user.getLatitude() != null && user.getLongitude() != null
                    && tradePost.getLatitude() != null && tradePost.getLongitude() != null) {
                double distance = DistanceUtil.calculateDistance(
                        user.getLatitude(), user.getLongitude(),
                        tradePost.getLatitude(), tradePost.getLongitude()
                );
                return TradePostDTO.fromEntityWithDistance(tradePost, distance);
            }
        }
        return TradePostDTO.fromEntity(tradePost);
    }

    public List<TradePostSimpleResponseDTO> getMyTradePosts(String username) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        List<TradePost> myPosts = tradePostRepository.findByUser(user);
        return myPosts.stream()
                .map(TradePostSimpleResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TradePostDTO> getAllTradePosts() {
        return tradePostRepository.findAll().stream()
                .map(TradePostDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TradePostDTO> getTradePostsByCategory(String category) {
        return tradePostRepository.findByCategory(category).stream()
                .map(TradePostDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TradePostDTO> searchTradePosts(String keyword) {
        List<TradePost> posts = tradePostRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        return posts.stream()
                .map(TradePostDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /* --------------------------- 통합 필터/정렬 --------------------------- */

    /**
     * 통합 필터/정렬 메서드
     * @param username   사용자명(거리 필터/거리 정렬 시 위치 추출에 사용)
     * @param distanceKm 거리(km) - null이면 적용 안 함
     * @param categories 카테고리 목록 - null/빈이면 적용 안 함
     * @param sort       정렬: LATEST(기본), UPDATED, DISTANCE, PRICE, PURCHASE_DATE
     */
    public List<TradePostDTO> getNearbyFlexible(
            String username, Double distanceKm, List<String> categories, String sort
    ) {
        UserEntity user = (username != null) ? userRepository.findByUsername(username) : null;
        Double lat = (user != null) ? user.getLatitude() : null;
        Double lng = (user != null) ? user.getLongitude() : null;

        // 위치 없으면 거리필터 무시
        if ((lat == null || lng == null) && distanceKm != null) distanceKm = null;

        boolean categoriesEmpty = (categories == null || categories.isEmpty());
        List<String> catsParam = categoriesEmpty ? List.of("_ALL_") : categories;

        List<TradePost> posts = tradePostRepository.findNearbyFlexible(
                lat, lng, distanceKm, catsParam, categoriesEmpty, TradePost.STATUS_ONGOING /* 진행중만; 전체면 null */
        );

        class P { TradePost p; Double d; P(TradePost p, Double d){ this.p=p; this.d=d; } }
        List<P> withDist = posts.stream().map(p -> {
            Double d = (lat != null && lng != null && p.getLatitude()!=null && p.getLongitude()!=null)
                    ? DistanceUtil.calculateDistance(lat, lng, p.getLatitude(), p.getLongitude()) : null;
            return new P(p, d);
        }).toList();

        Comparator<P> cmp;
        if ("DISTANCE".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing(x -> x.d == null ? Double.MAX_VALUE : x.d);
        } else if ("PRICE".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparingInt(x -> x.p.getPrice());
        } else if ("PURCHASE_DATE".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing((P x) -> x.p.getPurchaseDate(),
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else if ("UPDATED".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing((P x) -> x.p.getUpdatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else {
            cmp = Comparator.comparing((P x) -> x.p.getCreatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
        List<P> sorted = new java.util.ArrayList<>(withDist);
        sorted.sort(cmp);

        return sorted.stream()
                .map(x -> x.d != null ? TradePostDTO.fromEntityWithDistance(x.p, x.d) : TradePostDTO.fromEntity(x.p))
                .toList();
    }


    /* --------------------------- 위임(레거시 시그니처 대응) --------------------------- */

    // (선택) 여전히 호출되는 곳이 있으면 위임만 남겨둬도 됨
    public List<TradePostDTO> getNearbyTradePosts(String username, Double distanceKm) {
        return getNearbyFlexible(username, distanceKm, null, "LATEST");
    }

    public List<TradePostDTO> getNearbyByCategory(String username, double distanceKm, String category) {
        return getNearbyFlexible(username, distanceKm, Collections.singletonList(category), "LATEST");
    }

    public List<TradePostDTO> getNearbyPostsByMultipleCategories(UserEntity user, double distanceKm, List<String> categories) {
        String username = (user != null) ? user.getUsername() : null;
        return getNearbyFlexible(username, distanceKm, categories, "LATEST");
    }

    /* --------------------------- 인기/조회수/완료 처리 --------------------------- */

    public List<TradePostSimpleResponseDTO> getTop3PopularTradePosts() {
        Pageable pageable = PageRequest.of(0, 3);
        List<TradePost> topPosts = tradePostRepository.findTop3ByOrderByViewCountDesc(pageable);
        return topPosts.stream()
                .map(TradePostSimpleResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글을 찾을 수 없습니다."));
        post.setViewCount(post.getViewCount() + 1);
    }

    @Transactional
    public TradePost completeTradePost(Long postId, Long buyerId) {
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글이 존재하지 않습니다."));

        if (post.getStatus() == TradePost.STATUS_COMPLETED) {
            throw new IllegalStateException("이미 거래가 완료된 게시글입니다.");
        }

        UserEntity buyer = userRepository.findById(Math.toIntExact(buyerId))
                .orElseThrow(() -> new IllegalArgumentException("구매자가 존재하지 않습니다."));

        UserEntity seller = post.getUser();
        int price = post.getPrice();

        // 1) 구매자 포인트 차감
        pointService.usePoint(buyer, price, "거래 지출 - " + post.getTitle());
        // 2) 판매자 포인트 적립
        pointService.addPoint(seller, PointActionType.TRADE_COMPLETE, price, "거래 수익 - " + post.getTitle());

        // 3) 상태 변경 / 4) 구매자 기록
        post.setStatus(TradePost.STATUS_COMPLETED);
        post.setBuyer(buyer);

        return tradePostRepository.save(post);
    }

    public List<TradePostSimpleResponseDTO> getMyPurchasedPosts(String username) {
        UserEntity user = userRepository.findByUsername(username);
        List<TradePost> posts = tradePostRepository.findByBuyerAndStatus(user, TradePost.STATUS_COMPLETED);
        return posts.stream()
                .map(TradePostSimpleResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /* --------------------------- 사용자별/집계/관리자 --------------------------- */

    public List<TradePostSimpleResponseDTO> getPostsByUsername(String username) {
        return tradePostRepository.findByUser_Username(username).stream()
                .map(TradePostSimpleResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TradePostSimpleResponseDTO> getPostsByUsernameAndStatus(String username, int status) {
        return tradePostRepository.findByUser_UsernameAndStatus(username, status).stream()
                .map(TradePostSimpleResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BoardMonthlyStatsDTO> countTradePostMonthly(LocalDateTime startDate) {
        List<Object[]> raw = tradePostRepository.countTradePostMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<BoardMonthlyStatsDTO> countFreeTradePostMonthly(LocalDateTime startDate) {
        List<Object[]> raw = tradePostRepository.countFreeTradePostMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public Page<TradePostListResponseDTO> getTradePosts(int page, int size, Integer status, String sortBy, String keyword) {
        if (sortBy == null || sortBy.isEmpty()) sortBy = "updatedAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        Page<TradePost> tradePosts;
        if (keyword != null && !keyword.isBlank()) {
            tradePosts = tradePostRepository.findByStatusAndTitleKeyword(status, keyword, pageable);
        } else if (status == null) {
            tradePosts = tradePostRepository.findAll(pageable);
        } else {
            tradePosts = tradePostRepository.findByStatus(status, pageable);
        }

        return tradePosts.map(tp -> new TradePostListResponseDTO(
                tp.getTradePostId(),
                tp.getUser().getUsername(),
                tp.getTitle(),
                tp.getCreatedAt(),
                tp.getCategory(),
                tp.getStatus()
        ));
    }

    public TradePostDetailResponseDTO getTradePostDetail(Long postId) {
        TradePost tradePost = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래글이 존재하지 않습니다."));

        // 이미지 JSON -> 리스트 변환
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> imageUrls;
        try {
            imageUrls = objectMapper.readValue(tradePost.getImageUrls(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            imageUrls = Collections.emptyList();
        }

        int chatCount = chatRoomRepository.countByTradePostId(postId);

        return new TradePostDetailResponseDTO(
                tradePost.getTradePostId(),
                tradePost.getUser().getUsername(),
                tradePost.getTitle(),
                tradePost.getDescription(),
                tradePost.getCreatedAt(),
                imageUrls,
                tradePost.getLocation(),
                chatCount,
                tradePost.getViewCount(),
                tradePost.getStatus()
        );
    }

    @Transactional
    public void deletePostByAdmin(Long postId, String adminUsername, String reason) {
        TradePost post = tradePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("거래글이 존재하지 않습니다."));

        // 1) 연관 채팅 메시지 삭제
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByPost(post);
        for (ChatRoom room : chatRooms) {
            chatMessageRepository.deleteAllByRoomKey(room.getRoomKey());
        }

        // 2) 채팅방 삭제
        chatRoomRepository.deleteAll(chatRooms);

        // 3) 찜 삭제
        savedTradePostRepository.deleteAllByTradePost(post);

        // 4) 리뷰 삭제
        tpReviewRepository.deleteAllByTradePost(post);

        // 5) 거래글 삭제
        tradePostRepository.delete(post);

        // 6) 관리자 로그
        adminLogService.logAdminAction(
                adminUsername,
                "DELETE_POST",
                "TRADE_POST",
                postId,
                reason
        );
    }

    public UserProfileResponseDTO getUserProfile(UserEntity user) {
        int reviewCount = tpReviewRepository.countByUser(user);
        Double avgRating = tpReviewRepository.avgRatingByUser((long) user.getId());
        double rating = (avgRating != null) ? Math.round(avgRating * 10) / 10.0 : 0.0;
        int transactionCount = tradePostRepository.countByUser(user);

        return new UserProfileResponseDTO(
                user.getUsername(),
                rating,
                reviewCount,
                transactionCount
        );
    }
}
