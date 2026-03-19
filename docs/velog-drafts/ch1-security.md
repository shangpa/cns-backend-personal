# Spring Boot 리팩토링 1 — 비밀번호가 GitHub에 올라가기 전에

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

이번 챕터는 리팩토링에서 가장 먼저 손댄 보안 문제입니다. 두 가지를 다룹니다 — 자격증명 분리와 전역 예외처리.

---

## 1. 자격증명 분리

### 문제: 비밀번호가 코드에 있었다

```properties
# 리팩토링 전 application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=1234         # ← 하드코딩
spring.jwt.secret=supersecretkey123    # ← 하드코딩
```

이 파일이 GitHub에 올라가면 DB 비밀번호와 JWT 서명 키가 공개됩니다. 실제로 GitHub에서 `spring.datasource.password=`를 검색하면 수천 개의 저장소가 나옵니다. 공격자가 이 정보로 DB에 직접 접근하거나 JWT를 위조할 수 있습니다.

### 해결: 환경변수 + 로컬 전용 파일 분리

**application.properties (커밋 대상, 안전)**
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/test?serverTimezone=UTC&characterEncoding=UTF-8}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}   # 기본값 없음 — 반드시 주입 필요

spring.jwt.secret=${JWT_SECRET}

google.api.key=${GOOGLE_API_KEY:}
```

`${변수명:기본값}` 형식은 환경변수가 없을 때 기본값을 사용합니다.
`DB_PASSWORD`처럼 기본값이 없으면 환경변수가 없을 때 앱 실행이 실패합니다. **의도된 설계**입니다.

**application-local.properties (gitignore, 로컬 전용)**
```properties
# Local development credentials — DO NOT commit this file
DB_URL=jdbc:mysql://localhost:3306/test?serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=1234
JWT_SECRET=zjavb....(긴 문자열)
```

`.gitignore`에 `application-local.properties`를 추가하고, `application.properties`에서 import합니다.

```properties
spring.config.import=optional:classpath:application-local.properties
```

`optional:` 접두사는 파일이 없어도 에러를 내지 않습니다. 운영 환경에서는 이 파일 없이 OS 환경변수나 Docker Secret으로 주입합니다.

### 환경별 자격증명 주입 방법

| 환경 | 방법 |
|------|------|
| 로컬 개발 | `application-local.properties` |
| CI/CD (GitHub Actions) | Repository Secrets → 환경변수 |
| Docker | `docker run -e DB_PASSWORD=...` |
| Kubernetes | Secret → Pod 환경변수 |

핵심은 하나입니다. **비밀 값은 코드 저장소 밖에 있어야 한다.**

---

## 2. GlobalExceptionHandler

### 문제: 예외처리가 파편화되어 있었다

```java
// 어떤 컨트롤러: try-catch로 직접 처리
@PostMapping("/recipes")
public ResponseEntity<?> createRecipe(@RequestBody RecipeDTO dto) {
    try {
        Recipe r = recipeService.createRecipe(dto, username);
        return ResponseEntity.ok(r);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("서버 오류");  // 그냥 문자열
    }
}

// 다른 컨트롤러: 예외를 그냥 던짐
@GetMapping("/recipes/{id}")
public Recipe getRecipe(@PathVariable Long id) {
    return recipeService.getRecipeById(id); // 예외 그대로 전파
}
```

에러 응답 포맷이 컨트롤러마다 다르고, 같은 예외인데 HTTP 상태코드가 제각각으로 나올 수 있었습니다. 예외처리 코드가 비즈니스 로직과 섞이는 것도 문제였습니다.

### 개념: @RestControllerAdvice

`@RestControllerAdvice`는 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리하는 AOP 기반의 어드바이스입니다.

```
HTTP 요청
    ↓
Controller → 예외 발생
    ↓
HandlerExceptionResolver
    ↓
@RestControllerAdvice (GlobalExceptionHandler)
    ↓
@ExceptionHandler 메서드
    ↓
JSON 응답
```

### 해결: GlobalExceptionHandler 추가

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
```

가장 구체적인 예외가 먼저 매칭됩니다. `Exception.class`는 마지막 안전망입니다.

### 에러 응답 포맷 설계

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String code;      // 에러 코드 (프론트가 분기 처리에 사용)
    private final String message;   // 사람이 읽을 수 있는 메시지
    private final LocalDateTime timestamp;
}
```

실제 에러 응답:
```json
{
  "code": "NOT_FOUND",
  "message": "레시피를 찾을 수 없습니다: 99",
  "timestamp": "2024-03-17T14:30:00"
}
```

`code` 필드가 있으면 프론트엔드에서 `if (error.code === 'NOT_FOUND')`처럼 분기할 수 있습니다. `message`만 있으면 에러 분류가 어렵습니다.

### 전후 비교

**Before:** 컨트롤러마다 try-catch, 포맷 제각각
```java
catch (Exception e) {
    return ResponseEntity.status(500).body("서버 오류");
}
```

**After:** 서비스에서 예외만 던지면 GlobalExceptionHandler가 처리
```java
// 서비스
Recipe recipe = recipeRepository.findById(id)
    .orElseThrow(() -> new NoSuchElementException("레시피를 찾을 수 없습니다: " + id));

// 컨트롤러 — try-catch 없음
@GetMapping("/{id}")
public ResponseEntity<Recipe> getRecipe(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.getRecipeById(id));
}
```

컨트롤러가 훨씬 깔끔해졌습니다.

---

## 정리

| 변경 전 | 변경 후 |
|---------|---------|
| 비밀번호 하드코딩 | 환경변수 + application-local.properties |
| 예외처리 파편화 | GlobalExceptionHandler 중앙화 |
| 에러 응답 포맷 제각각 | `{code, message, timestamp}` 통일 |

> 챕터 2 → 코드 품질 — 로깅, 트랜잭션, 입력 검증
