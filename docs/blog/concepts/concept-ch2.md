# 챕터 2 개념 정리 — 로깅, 트랜잭션, 입력 검증

---

## [개념 1] SLF4J와 Logback의 관계 — Facade 패턴

**SLF4J** (Simple Logging Facade for Java) — 로깅 인터페이스(API). 구현체가 아님.
**Logback** — 실제 로깅 구현체. Spring Boot 기본 포함.

```
내 코드 → SLF4J API (Logger.info(...)) → Logback (실제로 파일/콘솔에 씀)
```

**왜 인터페이스를 쓰는가?**
구현체(Logback, Log4j2, JUL)가 바뀌어도 코드를 수정하지 않아도 됩니다.
라이브러리를 교체할 때 `import` 없이 의존성 파일만 바꾸면 됩니다.

이것이 **Facade 패턴** — 복잡한 구현 앞에 단순한 인터페이스를 두는 패턴.

```java
// 이 import는 SLF4J (인터페이스)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Logback이 실제로 동작하지만 코드에는 드러나지 않음
private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
```

Lombok `@Slf4j`는 이 두 줄을 자동 생성해 줍니다.

---

## [개념 2] `{}` 플레이스홀더 — 지연 평가(Lazy Evaluation)

```java
// 나쁜 방법 — 로그 레벨이 꺼져 있어도 문자열 결합이 항상 실행됨
log.debug("user = " + user.getName() + ", recipe = " + recipe.getTitle());

// 좋은 방법 — {} 사용 시 레벨이 꺼져 있으면 결합 자체를 실행하지 않음
log.debug("user = {}, recipe = {}", user.getName(), recipe.getTitle());
```

**왜 성능 차이가 나는가?**
- 문자열 `+` 연산 — Java가 `StringBuilder`를 생성하고 결합. 항상 실행.
- `{}` 플레이스홀더 — SLF4J가 먼저 "이 레벨이 활성화되어 있나?" 확인 후, 꺼져 있으면 인자 평가 자체를 건너뜀.

```java
// 내부적으로 이런 식으로 동작
if (log.isDebugEnabled()) {     // DEBUG가 꺼져 있으면
    log.debug("...", args);     // 이 블록 자체 실행 안 됨
}
```

---

## [개념 3] 영속성 컨텍스트 (Persistence Context)

JPA가 엔티티를 관리하는 메모리 공간. 트랜잭션 시작 시 생성되고 커밋 시 사라집니다.

```
트랜잭션 시작
    ↓
영속성 컨텍스트 생성
    ↓
findById(1L) 호출 → DB 조회 → 엔티티를 컨텍스트에 저장 + 스냅샷 저장
    ↓
엔티티 값 변경 (recipe.setTitle("새 제목"))
    ↓
트랜잭션 커밋
    ↓
Dirty Checking: 현재 엔티티 vs 스냅샷 비교 → 변경 감지
    ↓
UPDATE 쿼리 자동 실행
    ↓
영속성 컨텍스트 소멸
```

**Dirty Checking (변경 감지)이란?**
`save()`를 명시적으로 호출하지 않아도 트랜잭션 안에서 엔티티 필드를 바꾸면 커밋 시 자동으로 UPDATE가 나갑니다.

```java
@Transactional
public Recipe getRecipeById(Long id) {
    Recipe recipe = recipeRepository.findById(id).orElseThrow(...);
    recipe.setViewCount(recipe.getViewCount() + 1);  // save() 호출 없음
    return recipe;  // 트랜잭션 끝날 때 자동 UPDATE
}
```

---

## [개념 4] `@Transactional(readOnly = true)` 내부 동작

`readOnly = true`가 있으면:

1. **스냅샷 저장 생략** — 엔티티를 조회해도 비교용 원본 복사본을 만들지 않음
2. **Dirty Checking 생략** — 비교할 스냅샷이 없으니 변경 감지 자체를 안 함
3. **flush 모드 변경** — `FlushMode.NEVER`로 설정 → 트랜잭션 종료 시 DB에 반영 안 함

**주의:** `readOnly = true` 트랜잭션에서 `save()`를 호출하면 메서드 자체는 실행되지만 실제로 DB에 반영되지 않을 수 있습니다. (Hibernate 구현에 따라 `TransactionSystemException` 발생 가능)

**Master-Slave 구조에서의 추가 효과:**
```
readOnly = true  → Slave DB로 라우팅 (읽기 전용 복제본)
readOnly = false → Master DB로 라우팅 (쓰기 가능)
```
이 프로젝트는 단일 DB이므로 해당 없지만, 실제 운영 서비스에서는 중요한 최적화입니다.

---

## [개념 5] Bean Validation — `@Valid` 동작 흐름

**관련 어노테이션 패키지:** `jakarta.validation.constraints` (Spring Boot 3.x 기준)

```java
// DTO 필드에 제약 선언
public class RecipeDTO {
    @NotBlank(message = "제목은 필수입니다")       // null, "", "  " 모두 거부
    private String title;

    @NotNull(message = "조리 시간은 필수입니다")
    @Min(value = 1, message = "1분 이상이어야 합니다")
    private Integer cookingTime;
}

// 컨트롤러에서 @Valid 선언
@PostMapping("/recipes")
public ResponseEntity<?> create(@Valid @RequestBody RecipeDTO dto) { ... }
```

**동작 흐름:**
```
HTTP 요청 수신
    ↓
@RequestBody → JSON을 RecipeDTO로 역직렬화
    ↓
@Valid → Validator가 각 필드의 제약 조건 검사
    ↓ 위반 시
MethodArgumentNotValidException 발생
    ↓
GlobalExceptionHandler.handleValidation() 실행
    ↓
400 Bad Request + 에러 메시지 반환
```

**`@Valid` vs `@Validated` 차이:**

| | @Valid | @Validated |
|--|--------|-----------|
| 출처 | jakarta.validation (표준) | Spring 전용 |
| 중첩 검증 | 지원 (@Valid 다시 붙임) | 지원 |
| 그룹 검증 | 미지원 | 지원 (groups 파라미터) |
| 주로 쓰는 곳 | 컨트롤러 파라미터 | 서비스 메서드 파라미터 |

대부분의 경우 컨트롤러에서는 `@Valid`로 충분합니다.

---

## [개념 6] `@NotNull` vs `@NotEmpty` vs `@NotBlank` 차이

```java
String value = null;    // @NotNull 위반, @NotEmpty 위반, @NotBlank 위반
String value = "";      // @NotNull 통과, @NotEmpty 위반, @NotBlank 위반
String value = "   ";   // @NotNull 통과, @NotEmpty 통과, @NotBlank 위반 ← 공백만 있는 경우
String value = "abc";   // 모두 통과
```

사용자 입력 텍스트 필드에는 보통 `@NotBlank`가 적절합니다. 공백만 있는 입력도 막아야 하기 때문입니다.
