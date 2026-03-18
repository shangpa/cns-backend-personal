# 챕터 2. 코드 품질 — 로깅, 트랜잭션, 입력 검증

> 커버하는 커밋: `SLF4J 로깅`, `readOnly 트랜잭션`, `@Valid`

---

## 2-1. SLF4J 로깅

### 문제: `System.out.println`에 비밀번호가 찍혔다

```java
// 리팩토링 전
public String login(String username, String password) {
    System.out.println("username: " + username);
    System.out.println("password: " + password);  // 실제 비밀번호가 콘솔에 출력됨
    // ...
}
```

로그인 메서드에서 입력받은 비밀번호를 그대로 출력하고 있었습니다.
개발 환경에서는 눈에 잘 안 띄지만, 운영 서버 로그 파일에 이 내용이 남으면 심각한 문제입니다.

### `System.out.println` vs SLF4J 차이

| 항목 | System.out.println | SLF4J (+ Logback) |
|------|--------------------|--------------------|
| 로그 레벨 | 없음 | TRACE / DEBUG / INFO / WARN / ERROR |
| 운영 환경 제어 | 불가 | 레벨별 ON/OFF 가능 |
| 파일 출력 | 직접 구현 필요 | 설정 파일로 가능 |
| 성능 | 항상 실행 | 레벨 비활성화 시 평가 생략 |
| 컨텍스트 정보 | 없음 | 타임스탬프, 클래스명, 스레드명 자동 포함 |

`System.out.println`은 로그 레벨이 없기 때문에 "개발 중에만 보고 싶은 정보"와 "운영에서도 봐야 하는 오류"를 구분할 수 없습니다.

### 해결: Lombok `@Slf4j` 적용

Spring Boot는 기본으로 Logback + SLF4J를 포함하고 있어서 의존성 추가 없이 바로 사용 가능합니다.

```java
// Before
public class RecipeService {
    public Recipe createRecipe(RecipeDTO dto, String username) {
        System.out.println("createRecipe 호출됨");
        System.out.println("username = " + username);
        System.out.println("dto.title = " + dto.getTitle());
    }
}

// After
@Slf4j  // Lombok이 private static final Logger log = ... 를 자동 생성
@Service
public class RecipeService {
    public Recipe createRecipe(RecipeDTO dto, String username) {
        log.debug("createRecipe 호출됨");
        log.debug("username = {}", username);     // {} 플레이스홀더 사용
        log.debug("dto.title = {}", dto.getTitle());
    }
}
```

`{}` 플레이스홀더를 쓰는 이유: 로그 레벨이 비활성화되어 있으면 문자열 결합(+) 자체를 실행하지 않아 성능이 더 좋습니다.

### 로그 레벨 가이드

```java
log.trace("가장 상세한 정보 - 거의 안 씀");
log.debug("디버깅용 - 개발 환경에서만");
log.info("정상 동작 기록 - '레시피 생성됨', '사용자 로그인'");
log.warn("경고 - '요청한 리소스 없음', '유효하지 않은 입력'");
log.error("오류 - 예외 발생, 스택트레이스 포함");
```

운영 환경 `application.properties`:
```properties
logging.level.com.example.cns=INFO  # DEBUG 이하는 출력 안 됨
```

이렇게 설정하면 `log.debug()` 호출은 모두 무시됩니다.

---

## 2-2. `@Transactional(readOnly = true)`

### 개념: readOnly=true가 왜 성능에 좋은가

`@Transactional`을 붙이면 Spring이 메서드 실행 전 트랜잭션을 시작하고, 끝난 뒤 커밋합니다.
`readOnly = true`를 추가하면 JPA에게 "이 트랜잭션은 데이터를 변경하지 않는다"고 알립니다.

**Dirty Checking 비활성화**

JPA는 기본적으로 영속성 컨텍스트에 조회된 엔티티의 스냅샷을 저장하고, 트랜잭션 종료 시 변경 감지(Dirty Checking)를 실행합니다.

```
일반 @Transactional:
  1. 엔티티 조회 → 스냅샷 저장
  2. 로직 실행
  3. 트랜잭션 종료 시 → 모든 엔티티와 스냅샷 비교 (Dirty Checking)
  4. 변경된 것 UPDATE 쿼리 실행

@Transactional(readOnly = true):
  1. 엔티티 조회 → 스냅샷 저장 생략
  2. 로직 실행
  3. 트랜잭션 종료 시 → Dirty Checking 생략 (비교할 스냅샷이 없음)
```

조회만 하는 메서드에서 굳이 스냅샷을 만들고 비교할 필요가 없습니다.
엔티티 수가 많을수록 이 차이가 커집니다.

**DB 복제 구조에서의 추가 이점**

Master-Slave 구조에서 `readOnly = true`이면 Slave DB로 라우팅됩니다. Master는 쓰기 작업만 처리할 수 있어 부하가 줄어듭니다.

### 어떤 메서드에 붙였나

```java
// readOnly = true: 데이터 조회만 하는 메서드
@Transactional(readOnly = true)
public List<Recipe> getAllRecipes() { ... }

@Transactional(readOnly = true)
public List<RecipeSearchResponseDTO> getAllPublicRecipes(String sort) { ... }

@Transactional(readOnly = true)
public List<RecipeSearchResponseDTO> searchRecipesByTitle(String title) { ... }

// readOnly 없음: 데이터 변경이 있는 메서드
@Transactional
public Recipe createRecipe(RecipeDTO dto, String username) { ... }

@Transactional
public Recipe updateRecipe(Long id, RecipeDTO dto) { ... }

@Transactional
public void deleteRecipe(Long id) { ... }
```

### 주의: 쓰기 메서드에 readOnly = true 붙이면?

```java
@Transactional(readOnly = true)  // 잘못된 사용!
public Recipe createRecipe(RecipeDTO dto) {
    return recipeRepository.save(dto.toEntity());  // 실제로 INSERT가 필요한 메서드
}
```

Hibernate는 `readOnly = true` 트랜잭션에서 flush를 건너뜁니다.
`save()` 자체는 오류가 안 나지만 DB에 실제로 저장되지 않을 수 있습니다.
또는 `TransactionSystemException`이 발생할 수 있습니다.

**기준:** 메서드가 INSERT, UPDATE, DELETE 중 하나라도 한다면 `readOnly = true` 금지.

---

## 2-3. `@Valid` 입력 검증

### 문제: 빈 값이 서비스까지 내려왔다

```java
// 리팩토링 전 — 컨트롤러에서 검증 없음
@PostMapping("/recipes")
public ResponseEntity<?> createRecipe(@RequestBody RecipeDTO dto) {
    // dto.getTitle()이 null이거나 ""여도 그냥 서비스로 전달
    Recipe r = recipeService.createRecipe(dto, username);
    return ResponseEntity.ok(r);
}
```

이러면 DB INSERT 시 NOT NULL 제약조건 위반으로 예외가 발생하거나,
더 나쁜 경우 빈 제목으로 레시피가 저장됩니다.

### 개념: Bean Validation 동작 흐름

```
HTTP 요청 → @RequestBody RecipeDTO dto
    ↓
@Valid 있음 → Validator가 dto의 필드 검사
    ↓ (위반 시)
MethodArgumentNotValidException 발생
    ↓
GlobalExceptionHandler.handleValidation() 처리
    ↓
400 Bad Request + 에러 메시지 반환
```

### 해결: DTO에 검증 어노테이션 + 컨트롤러에 @Valid 추가

**DTO 클래스에 제약 조건 선언:**
```java
public class RecipeDTO {
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @Min(value = 1, message = "조리 시간은 1분 이상이어야 합니다")
    private Integer cookingTime;

    // ...
}
```

**컨트롤러에 @Valid 추가:**
```java
@PostMapping("/recipes")
public ResponseEntity<?> createRecipe(@Valid @RequestBody RecipeDTO dto,
                                       Authentication auth) {
    // 여기까지 오면 dto는 이미 유효성 검증 통과
    Recipe r = recipeService.createRecipe(dto, auth.getName());
    return ResponseEntity.ok(r);
}
```

**GlobalExceptionHandler에서 일관된 에러 응답:**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("VALIDATION_ERROR", message));
}
```

에러 응답:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "title: 제목은 필수입니다, category: 카테고리는 필수입니다"
}
```

---

## 정리

| 변경 전 | 변경 후 |
|---------|---------|
| System.out.println (비밀번호 포함) | log.debug / log.warn / log.error |
| 모든 메서드에 @Transactional | 조회 메서드: readOnly=true, 변경 메서드: 기본 |
| 입력 검증 없음 | @Valid + @NotBlank + GlobalExceptionHandler |

> 다음: [챕터 3 → Enum과 레거시 처리]
