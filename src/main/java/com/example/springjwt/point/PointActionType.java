package com.example.springjwt.point;

public enum PointActionType {
    RECIPE_WRITE, //레시피 작성
    COMMUNITY_POST, //커뮤니티 작성
    COMMUNITY_COMMENT,//커뮤니티 댓글
    COMMUNITY_LIKE_RECEIVED,//커뮤니티 좋아요 받기
    RECIPE_LIKE_RECEIVED,//레시피 좋아요 받기
    RECIPE_SCRAP_RECEIVED,//레시피 스크랩
    BEST_RECIPE_SELECTED,//베스트 레시피 선택
    FRIDGE_INPUT, // 냉장고 재료넣기
    TRADE_COMPLETE, //거래완료
    USE_POINT // 포인트 차감용
}