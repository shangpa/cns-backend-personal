package com.example.springjwt.tradepost;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradePostDTO {
    private Long tradePostId;       // 거래글 ID
    private String writer;          // 작성자 username (출력용)
    private String category;        // 카테고리
    private String title;           // 제목
    private int quantity;           // 수량
    private int price;              // 거래 가격
    private LocalDate purchaseDate; // 구매 날짜
    private String description;     // 설명
    private String location;        // 거래 희망 장소 (추후 사용)
    private String imageUrls;       // 이미지 URL 목록 (JSON 문자열)
    private String createdAt;
    private Double distance;
    private Double latitude;
    private Double longitude;

    // DTO → Entity (User는 외부에서 주입)
    public TradePost toEntity() {
        return TradePost.builder()
                .category(category)
                .title(title)
                .quantity(quantity)
                .price(price)
                .purchaseDate(purchaseDate)
                .description(description)
                .location(location)
                .imageUrls(imageUrls)
                .build();
    }

    // Entity → DTO
    public static TradePostDTO fromEntity(TradePost tradePost) {
        return TradePostDTO.builder()
                .tradePostId(tradePost.getTradePostId())
                .writer(tradePost.getUser().getUsername())
                .category(tradePost.getCategory())
                .title(tradePost.getTitle())
                .quantity(tradePost.getQuantity())
                .price(tradePost.getPrice())
                .purchaseDate(tradePost.getPurchaseDate())
                .description(tradePost.getDescription())
                .location(tradePost.getLocation())
                .imageUrls(tradePost.getImageUrls())
                .createdAt(tradePost.getCreatedAt().toString())
                .latitude(tradePost.getLatitude())
                .longitude(tradePost.getLongitude())
                .build();
    }

    // 거리 포함한 DTO 변환 메서드 따로 정의
    public static TradePostDTO fromEntityWithDistance(TradePost post, double distance) {
        return TradePostDTO.builder()
                .tradePostId(post.getTradePostId())
                .writer(post.getUser().getUsername())
                .category(post.getCategory())
                .title(post.getTitle())
                .quantity(post.getQuantity())
                .price(post.getPrice())
                .purchaseDate(post.getPurchaseDate())
                .description(post.getDescription())
                .location(post.getLocation())
                .imageUrls(post.getImageUrls())
                .createdAt(post.getCreatedAt().toString())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .distance(distance)
                .build();
    }

}