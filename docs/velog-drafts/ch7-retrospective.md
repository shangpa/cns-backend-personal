# Spring Boot 리팩토링 7 — 10개 커밋이 끝나고 남은 것들

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

팀 프로젝트 코드를 10개 커밋으로 리팩토링하는 시리즈의 마지막 챕터입니다. 변한 것들을 정리하고, 어려웠던 부분과 남은 과제를 돌아봅니다.

---

## 1. 10개 커밋으로 변한 것들

| 커밋 | 변경 전 | 변경 후 |
|------|---------|---------|
| 자격증명 분리 | DB 비밀번호 코드에 하드코딩 | 환경변수 + application-local.properties |
| GlobalExceptionHandler | 컨트롤러마다 try-catch, 포맷 제각각 | 중앙 예외처리, {code, message} 통일 |
| SLF4J 로깅 | System.out.println (비밀번호 출력 포함) | 레벨별 로그, 운영 환경에서 제어 가능 |
| readOnly 트랜잭션 | 모든 메서드 동일한 @Transactional | 조회: readOnly=true, 변경: 기본 |
| @Valid 입력 검증 | 검증 없이 서비스로 전달 | DTO 선언적 검증, 자동 400 응답 |
| TradeStatus Enum | `status == 1` 매직 넘버 | `TradeStatus.ONGOING` 명확한 이름 |
| Fridge @Deprecated | 그냥 존재하는 레거시 | @Deprecated 마킹, Pantry로 점진적 이전 |
| RecipeDraftService 분리 | RecipeService에 21개 메서드 | 초안 관리 5개 → 독립 서비스 |
| RecipeStatService 분리 | 위와 같음 | 통계 조회 6개 → 독립 서비스 |
| 테스트 코드 작성 | 테스트 없음 | 5개 테스트 클래스, 16개 테스트 케이스 |

---

## 2. 가장 어려웠던 부분

### Spring Security 6 + 테스트 환경

`@WebMvcTest`에서 인증을 주입하는 방법을 찾는 데 시간이 많이 걸렸습니다.

```
시도 1: .with(authentication(...)) → 403 (STATELESS 정책)
시도 2: .with(user("testuser")) → Principal 타입 불일치
시도 3: SecurityConfig exclude + TestSecurityConfig → 해결
```

문서만 봐서는 왜 안 되는지 바로 이해하기 어려웠습니다. 결국 세션 정책과 `authentication()` 포스트프로세서의 동작 원리를 이해하고 나서야 해결됐습니다.

### 분리 기준 찾기

RecipeService를 어떻게 나눌지 처음에는 막연했습니다. "초안은 따로 빼야 할 것 같다"는 느낌은 있었는데, 그 근거를 말로 표현하지 못했습니다.

SRP의 "바뀌는 이유가 하나여야 한다"는 기준을 적용하면서 명확해졌습니다.

> "초안 저장 방식이 바뀌면 이 클래스를 수정해야 하는가?"
> "통계 집계 방식이 바뀌면 이 클래스를 수정해야 하는가?"
> 두 질문에 모두 "예"라면 → 분리해야 한다.

이 질문을 기준으로 삼으니 어디를 나눠야 하는지 판단이 훨씬 쉬워졌습니다.

### 범위 조절

처음 계획에는 더 많은 것을 하려고 했습니다.
- 추천 서비스 분리
- 검색 서비스 리팩토링
- 통합 테스트 작성
- Pantry 완전 전환

하지만 범위를 제한하지 않으면 끝나지 않는다는 걸 알게 됐습니다. 이번 리팩토링의 범위를 "기능 추가 없이 코드 품질만"으로 정한 것이 10개 커밋을 실제로 완료할 수 있었던 이유입니다.

---

## 3. 컨트롤러에서 사라진 것들

```java
// Before
@PostMapping("/recipes")
public ResponseEntity<?> createRecipe(@RequestBody RecipeDTO dto) {
    try {
        Recipe r = recipeService.createRecipe(dto, username);
        return ResponseEntity.ok(r);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("서버 오류");
    }
}

// After
@PostMapping("/recipes")
public ResponseEntity<?> createRecipe(@Valid @RequestBody RecipeDTO dto, Authentication auth) {
    return ResponseEntity.ok(recipeService.createRecipe(dto, auth.getName()));
}
```

try-catch 블록, 에러 응답 직접 구성 코드, 비즈니스 로직이 섞였던 부분이 모두 사라졌습니다.

---

## 4. 다음에 할 것

### 단기
- **RECOMMEND 그룹 분리**: `suggestByType`, `getRecommendedRecipesByTitleKeywords` → `RecommendService`
- **예외 계층 구체화**: `NoSuchElementException` 대신 도메인별 커스텀 예외 (`RecipeNotFoundException`)

### 중기
- **Fridge → Pantry 완전 전환**: `@Deprecated` 마킹한 Fridge를 실제로 제거
- **통합 테스트 추가**: `@SpringBootTest` + Testcontainers로 실제 DB 연동 테스트

### 장기
- **API 문서화**: Swagger(springdoc) 또는 Spring REST Docs 도입

---

## 마치며

팀 프로젝트가 끝나고 코드를 다시 보는 것이 처음에는 부끄러웠습니다.
비밀번호가 콘솔에 찍히고, 예외처리는 없고, 한 클래스에 21개 메서드가 있었으니까요.

그런데 리팩토링을 진행하면서 오히려 이 부분들이 좋은 학습 재료가 됐습니다.
"왜 이렇게 하면 안 되는가"를 직접 경험하고 고쳐보는 것이 개념을 이해하는 가장 빠른 방법이었습니다.

```
보안 → 로깅 → 트랜잭션 → 검증 → 도메인 → 구조 → 테스트
```

이 순서로 진행한 것도 잘됐다고 생각합니다. 구조를 먼저 바꾸면 테스트 작성이 어렵고, 테스트가 없으면 구조 변경이 위험합니다. 기초부터 쌓고 테스트로 마무리한 순서가 맞았습니다.

---

*"동작하는 코드와 좋은 코드는 다르다. 그 차이를 아는 것이 시작이다."*
