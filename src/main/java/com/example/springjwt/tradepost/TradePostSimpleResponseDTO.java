package com.example.springjwt.tradepost;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradePostSimpleResponseDTO {
    private Long tradePostId;
    private String title;
    private String firstImageUrl; // 이미지 여러개 중 첫 번째
    private String createdAt;     // 작성 날짜
    private int price;
    private int status;           // 0: 거래중, 1: 거래완료

    public static TradePostSimpleResponseDTO fromEntity(TradePost tradePost) {
        return TradePostSimpleResponseDTO.builder()
                .tradePostId(tradePost.getTradePostId())
                .title(tradePost.getTitle())
                .firstImageUrl(tradePost.extractFirstImageUrl()) // 직접 첫번째꺼 추출
                .createdAt(tradePost.getPurchaseDate().toString())
                .price(tradePost.getPrice())
                .status(tradePost.getStatus())
                .build();
    }
}