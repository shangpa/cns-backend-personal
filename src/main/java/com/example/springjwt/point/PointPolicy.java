package com.example.springjwt.point;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PointPolicy {

    private final Map<PointActionType, Integer> pointMap = Map.of(
            PointActionType.RECIPE_WRITE, 10,               // 레시피 작성 시 10점
            PointActionType.COMMUNITY_POST, 2,              // 커뮤니티 게시글 작성 시 2점
            PointActionType.COMMUNITY_COMMENT, 1,           // 커뮤니티 댓글 작성 시 1점
            PointActionType.COMMUNITY_LIKE_RECEIVED, 1,     // 커뮤니티 글 좋아요 10개당 1점
            PointActionType.RECIPE_LIKE_RECEIVED, 1,        // 레시피 좋아요 10개당 1점
            PointActionType.RECIPE_SCRAP_RECEIVED, 1,       // 레시피 찜 10개당 1점
            PointActionType.BEST_RECIPE_SELECTED, 300,      // 인기 레시피 선정 시 300점
            PointActionType.FRIDGE_INPUT, 1,                // 냉장고 재료 10개 입력 시 1점
            PointActionType.TRADE_COMPLETE, 0,              //기본값 0 (amount는 직접 count로 넘김)
            PointActionType.USE_POINT, 0                        //차감은 계산 없음, 기록만
    );


    public int calculatePoint(PointActionType action, int count) {
        int base = pointMap.getOrDefault(action, 0);

        // 거래 완료 시, count = 거래 금액 → 그대로 적립
        if (action == PointActionType.TRADE_COMPLETE) {
            return count;
        }

        // 10개당 1점 방식
        if (action == PointActionType.COMMUNITY_LIKE_RECEIVED ||
                action == PointActionType.RECIPE_LIKE_RECEIVED ||
                action == PointActionType.RECIPE_SCRAP_RECEIVED ||
                action == PointActionType.FRIDGE_INPUT) {
            return (count / 10) * base;
        }

        return base;
    }

}
