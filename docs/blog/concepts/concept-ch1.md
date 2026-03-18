# 챕터 1 개념 정리 — 보안, 자격증명 분리, 전역 예외처리

---

## [개념 1] Spring Property Placeholder — `${변수명:기본값}`

```properties
spring.datasource.password=${DB_PASSWORD}
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/test}
```

`${DB_PASSWORD}` — 환경변수 `DB_PASSWORD` 값을 주입. 없으면 앱 실행 실패.
`${DB_URL:기본값}` — 환경변수 없을 때 `:` 뒤의 기본값 사용.

**Spring이 값을 찾는 순서 (우선순위 높은 것부터):**
1. OS 환경변수
2. JVM 시스템 프로퍼티 (`-Dkey=value`)
3. `application.properties` / `application.yml`
4. `spring.config.import`로 가져온 파일

---

## [개념 2] `spring.config.import` 와 `optional:` 접두사

```properties
spring.config.import=optional:classpath:application-local.properties
```

`spring.config.import` — 다른 설정 파일을 현재 파일에 병합해서 불러옴.
`optional:` — 파일이 없어도 에러 없이 무시. 없으면 에러가 남 → CI 환경에서 파일이 없어도 괜찮음.
`classpath:` — `src/main/resources/` 기준 경로.

---

## [개념 3] AOP(관점 지향 프로그래밍)와 `@RestControllerAdvice`

**AOP란?**
여러 클래스에 반복되는 관심사(로깅, 예외처리, 트랜잭션)를 한 곳에 모아 처리하는 프로그래밍 방식.

```
컨트롤러 A ──┐
컨트롤러 B ──┼──→ 예외 발생 → Advice(GlobalExceptionHandler)가 가로채서 처리
컨트롤러 C ──┘
```

`@ControllerAdvice` — 모든 컨트롤러에서 발생한 예외를 가로챔.
`@RestControllerAdvice` — `@ControllerAdvice` + `@ResponseBody`. 응답을 JSON으로 직렬화.

**내부 동작 흐름:**
```
예외 발생
    → DispatcherServlet이 HandlerExceptionResolverComposite 호출
    → ExceptionHandlerExceptionResolver가 @RestControllerAdvice 탐색
    → @ExceptionHandler(해당 예외 타입) 메서드 실행
    → ResponseEntity 반환 → JSON 직렬화 → HTTP 응답
```

---

## [개념 4] `@ExceptionHandler` 매칭 우선순위

여러 `@ExceptionHandler`가 있을 때 **가장 구체적인(하위) 타입이 먼저** 매칭됩니다.

```java
@ExceptionHandler(NoSuchElementException.class)  // 2순위 — 더 구체적
public ResponseEntity<?> handleNotFound(NoSuchElementException e) { ... }

@ExceptionHandler(Exception.class)               // 1순위 — 가장 넓음 (마지막 안전망)
public ResponseEntity<?> handleGeneral(Exception e) { ... }
```

`NoSuchElementException`은 `Exception`의 하위 타입이지만, 더 구체적인 핸들러가 먼저 매칭됩니다.
`Exception.class`는 위에서 매칭되지 않은 모든 예외를 잡는 catch-all 역할.

---

## [개념 5] HTTP 상태코드 설계

```
2xx — 성공
  200 OK           — 일반적인 성공
  201 Created      — 생성 성공 (POST 후 자원 생성됨)
  204 No Content   — 성공이지만 응답 본문 없음 (DELETE 후)

4xx — 클라이언트 오류
  400 Bad Request  — 잘못된 요청 (형식 오류, 유효성 실패)
  401 Unauthorized — 인증 필요 (로그인 안 됨)
  403 Forbidden    — 인가 실패 (로그인은 됐지만 권한 없음)
  404 Not Found    — 리소스 없음

5xx — 서버 오류
  500 Internal Server Error — 서버 내부 오류
```

**이 프로젝트의 매핑:**
```java
NoSuchElementException      → 404 Not Found
IllegalArgumentException    → 400 Bad Request
AccessDeniedException       → 403 Forbidden
MethodArgumentNotValidException → 400 Bad Request
Exception (그 외 모든 것)   → 500 Internal Server Error
```

---

## [개념 6] `@JsonInclude(JsonInclude.Include.NON_NULL)`

Jackson(JSON 직렬화 라이브러리)이 null 필드를 응답에서 제외하도록 설정.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;  // null이면 JSON에서 제외
}
```

```json
// timestamp가 null일 때
{ "code": "NOT_FOUND", "message": "..." }         // NON_NULL: timestamp 제외됨

// NON_NULL 없으면
{ "code": "NOT_FOUND", "message": "...", "timestamp": null }  // null이 그대로 나옴
```

**다른 옵션:**
```java
Include.ALWAYS    // 항상 포함 (기본값)
Include.NON_NULL  // null 제외
Include.NON_EMPTY // null + 빈 문자열 + 빈 컬렉션 제외
