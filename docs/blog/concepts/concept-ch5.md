# 챕터 5 개념 정리 — 테스트 코드, Mockito, Spring Security 테스트

---

## [개념 1] JUnit 5 주요 어노테이션

```java
@Test                 // 테스트 메서드 표시
@BeforeEach           // 각 @Test 전에 실행 (초기화)
@AfterEach            // 각 @Test 후에 실행 (정리)
@BeforeAll            // 클래스 내 모든 @Test 전에 한 번만 (static)
@DisplayName("설명")   // 테스트 이름 지정 (리포트에 표시)
@Nested               // 내부 클래스로 테스트 그룹화
@ExtendWith(...)      // 확장 기능 등록 (MockitoExtension 등)
```

**`@ExtendWith(MockitoExtension.class)` 역할:**
JUnit 5가 Mockito를 인식하도록 연결. 이 어노테이션이 없으면 `@Mock`, `@InjectMocks`가 동작하지 않습니다.

JUnit 4에서는 `@RunWith(MockitoJUnitRunner.class)` 였습니다. JUnit 5에서는 `@ExtendWith`로 바뀌었습니다.

---

## [개념 2] Mockito 내부 동작 — 프록시 기반

`@Mock`으로 생성된 객체는 실제 클래스가 아닌 **동적 프록시(바이트코드 조작)** 입니다.

```java
@Mock RecipeRepository recipeRepository;
```

`recipeRepository`는 실제 `RecipeRepository`처럼 보이지만, 실제로는 Mockito가 생성한 가짜 객체입니다.
모든 메서드 호출을 가로채서 기록하고, `when().thenReturn()`으로 정의한 동작을 실행합니다.

**Mock의 기본 반환값:**
`when()`을 정의하지 않으면:
```
참조 타입 (Object, List, String 등) → null
숫자 타입 (int, long 등)           → 0
boolean                            → false
컬렉션 (List, Set, Map)            → 빈 컬렉션 (null이 아님!)
```

---

## [개념 3] ArgumentMatcher — `any()`, `anyLong()`, `anyString()`

```java
// 구체적인 값 지정
when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
// → findById(10L)에만 반응. findById(99L)은 null 반환.

// ArgumentMatcher — 어떤 값이든 매칭
when(recipeRepository.findById(anyLong())).thenReturn(Optional.of(recipe));
// → findById(어떤 Long 값)에도 반응.

when(recipeRepository.save(any(Recipe.class))).thenReturn(recipe);
// → save(어떤 Recipe 타입)에 반응.
```

**주의: 하나라도 Matcher를 쓰면 모든 파라미터에 Matcher를 써야 함**
```java
// 잘못된 예 — 컴파일 에러는 아니지만 런타임 오류
when(repo.findByIdAndUserId(10L, anyInt())).thenReturn(...);  // 혼용 불가

// 올바른 예
when(repo.findByIdAndUserId(eq(10L), anyInt())).thenReturn(...);  // eq()로 감싸기
```

---

## [개념 4] AssertJ vs JUnit assertions

```java
// JUnit assertions
assertEquals(10L, result);
assertThrows(RuntimeException.class, () -> service.method());

// AssertJ (더 읽기 좋음, 체이닝 가능)
assertThat(result).isEqualTo(10L);
assertThatThrownBy(() -> service.method())
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("임시저장 레시피를 찾을 수 없습니다");
```

AssertJ가 선호되는 이유:
1. 실패 메시지가 더 명확합니다 ("expected 10 but was 99" 같은 형태)
2. 체이닝으로 여러 조건을 한 번에 검증할 수 있습니다
3. IDE 자동완성 지원이 좋습니다

---

## [개념 5] `@WebMvcTest` 가 로드하는 범위

```
@WebMvcTest 가 로드하는 것:
  - @Controller / @RestController
  - @ControllerAdvice / @RestControllerAdvice
  - @JsonComponent
  - Filter
  - WebMvcConfigurer
  - HandlerMethodArgumentResolver
  - Spring Security (기본 보안 설정 포함)

@WebMvcTest 가 로드하지 않는 것:
  - @Service
  - @Repository
  - @Component (일반)
  - JPA, DataSource
```

서비스와 저장소는 `@MockBean`으로 대체합니다.

```java
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {
    @MockBean RecipeService recipeService;           // 가짜로 대체
    @MockBean RecipeDraftService recipeDraftService; // 가짜로 대체
}
```

---

## [개념 6] `@MockBean` vs `@Mock` 차이

| | @Mock | @MockBean |
|--|-------|----------|
| 출처 | Mockito | Spring Boot Test |
| 용도 | Spring Context 없는 단위 테스트 | Spring Context가 있는 통합/슬라이스 테스트 |
| 사용 위치 | `@ExtendWith(MockitoExtension)` 클래스 | `@WebMvcTest`, `@SpringBootTest` 클래스 |
| 동작 | Mockito Mock 객체 생성 | Spring Bean으로 등록된 Mock 객체 생성 |

```java
// @ExtendWith(MockitoExtension) — @Mock 사용
@Mock RecipeRepository recipeRepository;
@InjectMocks RecipeDraftService service;

// @WebMvcTest — @MockBean 사용
@MockBean RecipeService recipeService;  // Spring Context의 Bean으로 등록됨
```

`@WebMvcTest`는 Spring Context를 로드하므로 의존성을 `@MockBean`으로 등록해야 컨트롤러에 주입됩니다.

---

## [개념 7] Spring Security 6의 기본 세션 정책

Spring Security 6에서 REST API용 설정의 기본 세션 정책은 **STATELESS**입니다.

```java
// 운영 SecurityConfig
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
);
```

**STATELESS란?**
- 서버가 세션을 생성하지 않습니다.
- 모든 요청은 JWT 등 토큰으로만 인증됩니다.
- 확장성에 유리합니다 (서버를 여러 대 띄워도 세션 공유 불필요).

**테스트 문제의 원인:**
`authentication()` 포스트프로세서는 `SecurityContext`를 HTTP 세션에 저장합니다.
STATELESS 정책에서는 세션을 만들지 않으므로, 저장된 인증 정보를 꺼낼 수 없어 403이 납니다.

**해결 방법:** 테스트 전용 SecurityConfig에서 세션 정책을 명시하지 않으면 기본값(IF_REQUIRED — 세션 사용)이 적용됩니다.

---

## [개념 8] `@TestConfiguration` vs `@Configuration`

```java
@TestConfiguration
@EnableWebSecurity
static class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception { ... }
}
```

| | @Configuration | @TestConfiguration |
|--|---------------|-------------------|
| 적용 범위 | 앱 전체 | 테스트 클래스에서 명시적으로 import 시에만 |
| 기존 Bean 관계 | 기존 Bean 오버라이드 가능 | 기존 Bean에 추가/오버라이드 |
| 자동 스캔 | 됨 | 안 됨 (@Import 필요) |

`@TestConfiguration`을 쓰면 테스트 외부에는 영향을 주지 않습니다.
`@Import(TestSecurityConfig.class)`로 명시적으로 불러와야 적용됩니다.

---

## [개념 9] `Collections.singletonList` vs `List.of`

**`List.of()`** (Java 9+) — 불변 리스트. varargs로 여러 원소 가능.
```java
List<String> list = List.of("a", "b", "c");
list.add("d");  // UnsupportedOperationException — 불변!
```

**`Collections.singletonList()`** — 정확히 1개 원소인 불변 리스트.
```java
List<String> list = Collections.singletonList("a");
```

**Object[] 와 List.of 조합 시 타입 추론 문제:**
```java
// 문제 — Object[]가 varargs T의 단일 원소로 해석될 수 있음
List.of(new Object[]{"2024-01", 5L}, new Object[]{"2024-02", 3L})
// 타입: List<Object[]> 가 되어야 하는데 추론이 불안정할 수 있음

// 안전한 방법
List<Object[]> rows = new ArrayList<>();
rows.add(new Object[]{"2024-01", 5L});
rows.add(new Object[]{"2024-02", 3L});

// 단일 원소인 경우
List<Object[]> row = Collections.singletonList(new Object[]{"2024-01", 5L});
```

---

## [개념 10] `standaloneSetup` vs `@WebMvcTest` 선택 기준

```java
// standaloneSetup — Spring Context 없이 빠른 테스트
MockMvcBuilders.standaloneSetup(new TestController())
    .setControllerAdvice(new GlobalExceptionHandler())
    .build();

// @WebMvcTest — Spring Context 포함 (Security, Filter 등)
@WebMvcTest(RecipeController.class)
class RecipeControllerTest { ... }
```

| 상황 | 선택 |
|------|------|
| ControllerAdvice 예외 매핑 테스트 | standaloneSetup |
| 인증/인가 테스트 (403, 401) | @WebMvcTest |
| 필터, 인터셉터 포함 테스트 | @WebMvcTest |
| 빠른 단순 라우팅 테스트 | standaloneSetup |

`GlobalExceptionHandlerTest`에서 `standaloneSetup`을 쓴 이유:
Security, 필터 없이 순수하게 "예외 → HTTP 상태코드 매핑"만 확인하면 되기 때문입니다.
