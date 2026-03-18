# 챕터 0. 들어가며 — 왜 리팩토링했나

> 이 시리즈는 Spring Boot 백엔드 프로젝트를 10개 커밋으로 리팩토링한 과정을 기록한 글입니다.
> 보안, 코드 품질, 도메인 모델, 서비스 분리, 테스트 순서로 진행했습니다.

---

## 원래 코드 상태

팀 프로젝트가 끝나고 백엔드 코드를 다시 봤을 때 눈에 띈 것들이 있었습니다.

```java
// 비밀번호가 코드에 그대로
spring.datasource.password=1234

// 로그 대신 System.out.println
System.out.println("username: " + username);
System.out.println("password: " + password); // 비밀번호까지 찍힘

// RecipeService에 메서드가 21개
public class RecipeService {
    // 레시피 생성, 수정, 삭제
    // 초안 생성, 초안 수정, 초안 발행, 초안 삭제
    // 통계 조회 (카테고리별, 월별, 연도별...)
    // 추천 레시피
    // 예상 재료 조회
    // 썸네일 생성
    // 관리자 삭제
    // ... 21개
}

// 예외처리는 컨트롤러마다 try-catch
try {
    recipeService.createRecipe(dto, username);
} catch (Exception e) {
    return ResponseEntity.status(500).body("서버 오류");
}
```

동작은 했습니다. 기능 구현이 우선이던 팀 프로젝트 특성상 어쩔 수 없는 부분도 있었습니다.
그런데 코드를 보면서 "이게 배포된 상태라면?" 이라는 질문이 생겼습니다.

---

## 리팩토링을 결심한 이유

**1. 보안 문제가 실제였다**
DB 비밀번호, JWT Secret이 코드에 하드코딩되어 있으면 GitHub에 올리는 순간 노출됩니다.
`System.out.println`에 username/password가 찍히면 서버 로그를 보는 사람 누구나 알 수 있습니다.

**2. 예외처리가 일관성이 없었다**
어떤 컨트롤러는 try-catch로 500을 내보내고, 어떤 컨트롤러는 예외를 그냥 터뜨렸습니다.
프론트엔드 입장에서 에러 응답 포맷이 제각각이면 처리하기 어렵습니다.

**3. RecipeService가 너무 많은 일을 했다**
21개 메서드 중에 "초안 관련"과 "통계 관련"은 완전히 다른 이유로 바뀝니다.
초안 로직이 바뀌어도 통계 테스트를 다시 돌아야 하는 상황이었습니다.

**4. 테스트 코드가 없었다**
기능 추가 후 "됩니다"를 확인하는 방법이 직접 API를 호출하는 것뿐이었습니다.

---

## 리팩토링 로드맵

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

기능을 건드리지 않고 코드 품질만 개선하는 것이 목표였습니다.

---

다음 챕터에서는 가장 먼저 해결한 보안 문제부터 시작합니다.

> 시리즈: [챕터 1 → 보안 — 자격증명 분리 + 전역 예외처리]
