// src/main/java/com/example/springjwt/search/SortKey.java
package com.example.springjwt.search;

public enum SortKey {
    viewCount,   // 조회수 내림차순
    likes,       // 찜수 내림차순  (엔티티에 컬럼 없으면 주석 참고)
    latest,      // 최신순 (createdAt desc)
    shortTime,   // 조리시간 짧은순 (asc)
    longTime;    // 조리시간 긴순 (desc)

    /** 정렬에 사용할 엔티티 필드명 매핑 */
    public String property() {
        return switch (this) {
            case viewCount -> "viewCount";
            case likes     -> "likes";        // ⚠️ Recipe 엔티티에 실제 필드가 없으면 별도 쿼리 필요
            case latest    -> "createdAt";
            case shortTime -> "cookingTime";
            case longTime  -> "cookingTime";
        };
    }

    /** 오름/내림차순 */
    public boolean ascending() {
        return this == shortTime;
    }
}
