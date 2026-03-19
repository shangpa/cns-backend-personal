# 팀 프로젝트 코드를 다시 뜯어봤다 — Spring Boot 리팩토링 시리즈 시작

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

팀 프로젝트가 끝나고 코드를 다시 열었을 때 솔직히 좀 부끄러웠습니다.

```java
// 비밀번호가 코드에 그대로
spring.datasource.password=1234

// 로그 대신 System.out.println
System.out.println("username: " + username);
System.out.println("password: " + password); // 비밀번호까지 찍힘

// RecipeService에 메서드가 21개
public class RecipeService {
    // 레시피 생성, 수정, 삭제
    // 초안 생성, 수정, 발행, 삭제
    // 통계 조회 (카테고리별, 월별, 연도별...)
    // 추천, 검색, 관리자 삭제...
    // ... 21개
}

// 예외처리는 컨트롤러마다 제각각
try {
    recipeService.createRecipe(dto, username);
} catch (Exception e) {
    return ResponseEntity.status(500).body("서버 오류");
}
```

동작은 했습니다. 기능 구현이 우선이던 팀 프로젝트 특성상 어쩔 수 없는 부분도 있었고요.

그런데 코드를 보면서 **"이게 배포된 상태라면?"** 이라는 질문이 생겼습니다.

---

## 1. 왜 리팩토링을 결심했나

**보안 문제가 실제였습니다.**

DB 비밀번호, JWT Secret이 코드에 하드코딩되어 있으면 GitHub에 올리는 순간 노출됩니다. 실제로 GitHub에서 `spring.datasource.password=`를 검색하면 수천 개의 저장소가 나옵니다. `System.out.println`에 password가 찍히는 건 더 심각합니다. 서버 로그를 볼 수 있는 사람이라면 누구나 볼 수 있으니까요.

**예외처리가 일관성이 없었습니다.**

어떤 컨트롤러는 try-catch로 500을 내보내고, 어떤 컨트롤러는 예외를 그냥 터뜨렸습니다. 프론트엔드 입장에서 에러 응답 포맷이 제각각이면 처리하기가 정말 어렵습니다.

**RecipeService가 너무 많은 일을 했습니다.**

21개 메서드 중에 "초안 관련"과 "통계 관련"은 완전히 다른 이유로 바뀝니다. 초안 로직이 바뀌어도 통계 테스트를 다시 돌아야 하는 상황이었습니다.

**테스트 코드가 없었습니다.**

기능 추가 후 "됩니다"를 확인하는 방법이 직접 API를 호출하는 것뿐이었습니다.

---

## 2. 10개 커밋 로드맵

범위를 먼저 정했습니다. **기능 추가 없이 코드 품질만** 개선하는 것.

| 커밋 | 내용 |
|------|------|
| 1 | 자격증명 환경변수 분리 |
| 2 | GlobalExceptionHandler 추가 |
| 3 | SLF4J 로깅으로 교체 |
| 4 | readOnly 트랜잭션 적용 |
| 5 | @Valid 입력 검증 추가 |
| 6 | TradeStatus Enum 도입 |
| 7 | Fridge @Deprecated 처리 |
| 8 | RecipeDraftService 분리 |
| 9 | RecipeStatService 분리 |
| 10 | 테스트 코드 작성 |

순서도 의도적으로 잡았습니다. 구조를 먼저 바꾸면 테스트 작성이 어렵고, 테스트가 없으면 구조 변경이 위험합니다. 그래서 보안 → 로깅 → 트랜잭션 → 검증 → 도메인 → 구조 → 테스트 순서로 진행했습니다.

---

다음 챕터에서는 API URL 설계부터 시작합니다.

> 챕터 1 → RESTful API 리팩토링 — URL에서 동사를 없앤 이야기
