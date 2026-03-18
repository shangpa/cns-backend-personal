# 챕터 5. 테스트 코드 — 처음 작성하면서 배운 것들

> 커버하는 커밋: 테스트 커밋 4개
> (JWTUtilTest, GlobalExceptionHandlerTest, RecipeDraftServiceTest, RecipeStatServiceTest, RecipeControllerTest)

---

## 각 테스트 파일 요약

| 파일 | 방법 | 검증 내용 |
|------|------|---------|
| `JWTUtilTest` | 순수 단위 테스트 | JWT 생성/파싱/만료 검증 |
| `GlobalExceptionHandlerTest` | standaloneSetup | 예외→HTTP 상태코드/에러코드 매핑 |
| `RecipeDraftServiceTest` | Mockito | 초안 생성/조회 비즈니스 로직 |
| `RecipeStatServiceTest` | Mockito | 통계 집계 로직, 파라미터 검증 |
| `RecipeControllerTest` | @WebMvcTest | HTTP 인증/인가 + 컨트롤러 라우팅 |

---

## 5-1. Mockito 기본 사용

### `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`

서비스 테스트는 DB 연결 없이 Mock 객체로 의존성을 대체합니다.

```java
@ExtendWith(MockitoExtension.class)   // Mockito를 JUnit 5에 연결
class RecipeDraftServiceTest {

    @Mock RecipeRepository recipeRepository;                   // 가짜 Repository
    @Mock IngredientMasterRepository ingredientMasterRepository;

    @InjectMocks RecipeDraftService recipeDraftService;        // 위 Mock들을 주입받은 실제 서비스
```

`@InjectMocks`는 `@Mock`으로 선언된 객체들을 생성자나 필드 주입으로 넣어줍니다.
`RecipeDraftService`의 생성자 파라미터 타입과 `@Mock` 타입이 일치해야 합니다.

### `when()` — Mock 동작 정의

```java
// recipeRepository.findDraftWithIngredients(99L, 1) 호출 시 Optional.empty() 반환
when(recipeRepository.findDraftWithIngredients(99L, 1))
        .thenReturn(Optional.empty());
```

### `verify()` — 메서드 호출 여부 검증

```java
verify(recipeRepository).save(any(Recipe.class));
// recipeRepository.save()가 정확히 1번 호출됐는지 검증
```

---

## 5-2. `thenAnswer` vs `thenReturn`

### 문제: `thenReturn`으로는 JPA `save()` 이후 ID를 채울 수 없다

실제 JPA 환경에서는 `save()` 호출 후 DB가 ID(AUTO_INCREMENT)를 채워줍니다.

```java
Recipe saved = recipeRepository.save(entity);
return saved.getRecipeId();   // DB가 채워준 ID를 반환
```

Mock에서 `thenReturn`을 쓰면:

```java
when(recipeRepository.save(any(Recipe.class)))
        .thenReturn(new Recipe());   // 새 객체 반환 — recipeId는 null!
```

`save(entity)`를 호출했지만, 반환된 건 **다른 새 객체**입니다.
서비스 코드는 `entity.getRecipeId()`를 쓰는데 이 값은 여전히 null입니다.

### 해결: `thenAnswer`로 인자를 직접 수정

```java
when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
    Recipe r = invocation.getArgument(0);   // 실제로 넘어온 entity를 꺼내서
    r.setRecipeId(10L);                     // ID를 직접 세팅
    return r;                               // 같은 객체 반환
});

Long id = recipeDraftService.createDraftTransactional(dto, user);

assertThat(id).isEqualTo(10L);   // 통과
```

**`thenReturn`과 `thenAnswer` 차이 요약:**

| | thenReturn | thenAnswer |
|--|-----------|-----------|
| 반환 시점 | 테스트 정의 시 고정 | 실제 호출 시점에 실행 |
| 인자 접근 | 불가 | `invocation.getArgument(n)` |
| 용도 | 단순 반환값 | 인자 기반 동적 처리, 상태 변경 |

---

## 5-3. `@WebMvcTest` vs `standaloneSetup`

### `standaloneSetup` — GlobalExceptionHandlerTest에서 사용

```java
@BeforeEach
void setUp() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
}
```

Spring Context 없이 컨트롤러 + 어드바이스만 등록합니다.
Spring Security, 필터, 인터셉터 없이 순수하게 예외 처리 로직만 테스트할 때 적합합니다.
빠릅니다 (Context 로딩 없음).

### `@WebMvcTest` — RecipeControllerTest에서 사용

```java
@WebMvcTest(
    controllers = RecipeController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class
    )
)
@Import(RecipeControllerTest.TestSecurityConfig.class)
class RecipeControllerTest {
    @Autowired MockMvc mockMvc;

    @MockBean RecipeService recipeService;
    @MockBean RecipeDraftService recipeDraftService;
    ...
}
```

Web 계층 (Controller, Filter, Interceptor, ControllerAdvice)만 로드합니다.
Service, Repository, JPA는 로드하지 않습니다 (`@MockBean`으로 대체).
Spring Security도 포함됩니다 — 인증/인가 테스트에 적합합니다.

---

## 5-4. 오류 기록: 403 문제와 Spring Security 6

### 문제: `authentication()` 포스트프로세서가 동작하지 않았다

```java
mockMvc.perform(post("/api/recipes/drafts")
        .with(authentication(authOf(1)))   // 인증 주입 시도
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"테스트\"}"))
.andExpect(status().isOk());   // 실제 결과: 403!
```

**원인:** Spring Security 6의 기본 세션 정책은 `STATELESS`입니다.
`authentication()` 포스트프로세서는 세션에 인증 정보를 저장하는 방식인데,
STATELESS 정책에서는 세션이 없으므로 인증 정보가 전달되지 않았습니다.

**해결:** 실제 `SecurityConfig`를 테스트에서 제외하고, 세션 기반 테스트 전용 Security 설정을 import합니다.

```java
@WebMvcTest(
    controllers = RecipeController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class    // 실제 STATELESS 설정 제외
    )
)
@Import(RecipeControllerTest.TestSecurityConfig.class)   // 테스트용 설정 주입
class RecipeControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            // sessionManagement 설정 없음 → 기본값(IF_REQUIRED) → 세션 사용
            return http.build();
        }
    }
}
```

핵심은 `sessionManagement`를 명시하지 않으면 Spring Security가 기본 세션 정책(IF_REQUIRED)을 씁니다.
이 상태에서 `authentication()` 포스트프로세서가 세션에 인증 정보를 저장하고, 테스트가 정상 동작합니다.

---

## 5-5. 오류 기록: Lombok 컴파일 에러

### 문제: test scope에서 Lombok이 안 됐다

```gradle
// build.gradle — 팀 프로젝트 설정
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    // testCompileOnly, testAnnotationProcessor 없음!
}
```

`compileOnly`는 main 소스셋에만 적용됩니다. test 소스셋에서 `@Getter`/`@Setter` 등을 쓰면 컴파일 에러가 납니다.

**올바른 해결 방법:**
```gradle
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'           // 추가
    testAnnotationProcessor 'org.projectlombok:lombok'   // 추가
}
```

**이 프로젝트에서 선택한 방법:** 테스트 클래스에서 Lombok을 쓰지 않고 수동으로 getter/setter를 작성했습니다. (build.gradle을 건드리지 않기 위해)

---

## 5-6. 오류 기록: `List.of(new Object[]{...})` 타입 추론 오류

### 문제

```java
// RecipeStatServiceTest — 컴파일 에러 발생
when(recipeRepository.countByYear(2024))
    .thenReturn(List.of(new Object[]{1, 5L}, new Object[]{2, 3L}));
//            ^^^^^^^^^
// List.of(T... elements)에서 Object[]가 단일 T로 해석됨
// → List<Object[]> 가 아닌 List<Object[][]> 로 추론되는 문제
```

`List.of(Object[], Object[])`에서 두 `Object[]`가 varargs `T...`의 각 원소로 올바르게 해석되어야 하는데,
제네릭 타입 추론이 `List<Object[]>`로 정확히 맞아 떨어지지 않는 상황이 있습니다.

### 해결: `Collections.singletonList` 사용

```java
// 단일 원소일 때
List<Object[]> rows = Collections.singletonList(new Object[]{"2024-01-15", 2L});
when(recipeRepository.countByDateRange(...))
        .thenReturn(rows);

// 여러 원소일 때
List<Object[]> rows = new ArrayList<>();
rows.add(new Object[]{1, 5L});
rows.add(new Object[]{2, 3L});
when(recipeRepository.countByYear(2024)).thenReturn(rows);
```

`Collections.singletonList`는 정확히 1개의 원소를 담는 불변 리스트를 반환합니다.
varargs 대신 명시적 타입을 쓰기 때문에 추론 문제가 없습니다.

---

## 5-7. 실제 테스트 코드 — RecipeDraftServiceTest 전체

```java
@ExtendWith(MockitoExtension.class)
class RecipeDraftServiceTest {

    @Mock RecipeRepository recipeRepository;
    @Mock IngredientMasterRepository ingredientMasterRepository;

    @InjectMocks RecipeDraftService recipeDraftService;

    @Test
    void createDraft_success() {
        // given
        RecipeDTO dto = RecipeDTO.builder()
                .title("임시 레시피")
                .build();

        UserEntity user = new UserEntity();
        user.setId(1);

        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setRecipeId(10L);   // DB가 줄 ID를 시뮬레이션
            return r;
        });

        // when
        Long id = recipeDraftService.createDraftTransactional(dto, user);

        // then
        assertThat(id).isEqualTo(10L);
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void getMyDraftById_unauthorized_throwsException() {
        // given
        UserEntity user = new UserEntity();
        user.setId(1);

        when(recipeRepository.findDraftWithIngredients(99L, 1))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> recipeDraftService.getMyDraftById(99L, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("임시저장 레시피를 찾을 수 없습니다");
    }
}
```

given / when / then 패턴으로 각 테스트의 의도가 명확합니다.

---

## 정리

| 테스트 유형 | 언제 사용 | 특징 |
|------------|---------|-----|
| 순수 단위 테스트 | 외부 의존 없는 유틸 클래스 | 가장 빠름 |
| Mockito (`@Mock`) | 서비스 레이어 로직 | DB 없이 격리된 테스트 |
| `standaloneSetup` | 예외처리, ControllerAdvice | Spring Context 불필요 |
| `@WebMvcTest` | 컨트롤러 + Security | HTTP 레이어 전체 테스트 |

> 다음: [챕터 6 → 마무리 — 리팩토링 회고]
