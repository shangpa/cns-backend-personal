# Spring Boot 리팩토링 6 — 테스트 코드를 처음 작성하면서 막혔던 것들

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

테스트 코드를 처음 제대로 작성해봤습니다. 예상보다 막히는 지점이 많았고, 그 과정에서 배운 게 더 많았습니다. 이번 챕터는 Mockito 기본부터 Spring Security 6 이슈까지 겪었던 것들을 정리합니다.

---

## 1. 작성한 테스트 파일 구성

| 파일 | 방법 | 검증 내용 |
|------|------|---------|
| `JWTUtilTest` | 순수 단위 테스트 | JWT 생성/파싱/만료 검증 |
| `GlobalExceptionHandlerTest` | standaloneSetup | 예외→HTTP 상태코드/에러코드 매핑 |
| `RecipeDraftServiceTest` | Mockito | 초안 생성/조회 비즈니스 로직 |
| `RecipeStatServiceTest` | Mockito | 통계 집계 로직, 파라미터 검증 |
| `RecipeControllerTest` | @WebMvcTest | HTTP 인증/인가 + 컨트롤러 라우팅 |

---

## 2. Mockito 기본 사용

서비스 테스트는 DB 연결 없이 Mock 객체로 의존성을 대체합니다.

```java
@ExtendWith(MockitoExtension.class)   // Mockito를 JUnit 5에 연결
class RecipeDraftServiceTest {

    @Mock RecipeRepository recipeRepository;    // 가짜 Repository
    @Mock IngredientMasterRepository ingredientMasterRepository;

    @InjectMocks RecipeDraftService recipeDraftService;  // 위 Mock들을 주입받은 실제 서비스
}
```

`@InjectMocks`는 `@Mock`으로 선언된 객체들을 생성자나 필드 주입으로 넣어줍니다.
`RecipeDraftService`의 생성자 파라미터 타입과 `@Mock` 타입이 일치해야 합니다.

**when()으로 Mock 동작 정의:**
```java
when(recipeRepository.findDraftWithIngredients(99L, 1))
        .thenReturn(Optional.empty());
```

**verify()로 메서드 호출 여부 검증:**
```java
verify(recipeRepository).save(any(Recipe.class));
// recipeRepository.save()가 정확히 1번 호출됐는지 검증
```

---

## 3. thenAnswer vs thenReturn

### 막혔던 지점: thenReturn으로는 JPA save() 이후 ID를 채울 수 없다

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

`save(entity)`를 호출했지만, 반환된 건 **다른 새 객체**입니다. 서비스 코드는 `entity.getRecipeId()`를 쓰는데 이 값은 여전히 null입니다.

### 해결: thenAnswer로 인자를 직접 수정

```java
when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
    Recipe r = invocation.getArgument(0);   // 실제로 넘어온 entity를 꺼내서
    r.setRecipeId(10L);                     // ID를 직접 세팅
    return r;                               // 같은 객체 반환
});

Long id = recipeDraftService.createDraftTransactional(dto, user);

assertThat(id).isEqualTo(10L);   // 통과
```

| | thenReturn | thenAnswer |
|--|-----------|-----------|
| 반환 시점 | 테스트 정의 시 고정 | 실제 호출 시점에 실행 |
| 인자 접근 | 불가 | `invocation.getArgument(n)` |
| 용도 | 단순 반환값 | 인자 기반 동적 처리, 상태 변경 |

---

## 4. @WebMvcTest vs standaloneSetup

### standaloneSetup — GlobalExceptionHandlerTest에서 사용

```java
@BeforeEach
void setUp() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
}
```

Spring Context 없이 컨트롤러 + 어드바이스만 등록합니다. Spring Security, 필터 없이 예외 처리 로직만 순수하게 테스트할 때 적합하고, Context 로딩이 없어서 빠릅니다.

### @WebMvcTest — RecipeControllerTest에서 사용

```java
@WebMvcTest(controllers = RecipeController.class, ...)
class RecipeControllerTest {
    @Autowired MockMvc mockMvc;

    @MockBean RecipeService recipeService;
    @MockBean RecipeDraftService recipeDraftService;
}
```

Web 계층(Controller, Filter, Interceptor, ControllerAdvice)만 로드합니다. Service, Repository, JPA는 로드하지 않고 `@MockBean`으로 대체합니다. Spring Security도 포함되어서 인증/인가 테스트에 적합합니다.

---

## 5. 삽질: Spring Security 6 + @WebMvcTest에서 403이 계속 났다

### 문제

```java
mockMvc.perform(post("/api/recipes/drafts")
        .with(authentication(authOf(1)))   // 인증 주입 시도
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"테스트\"}"))
.andExpect(status().isOk());   // 실제 결과: 403!
```

### 원인

Spring Security 6의 기본 세션 정책은 `STATELESS`입니다.
`authentication()` 포스트프로세서는 세션에 인증 정보를 저장하는 방식인데, STATELESS 정책에서는 세션이 없으므로 인증 정보가 전달되지 않았습니다.

```
시도 1: .with(authentication(...)) → 403 (STATELESS 정책)
시도 2: .with(user("testuser")) → Principal 타입 불일치
시도 3: SecurityConfig exclude + TestSecurityConfig → 해결
```

### 해결

실제 `SecurityConfig`를 테스트에서 제외하고, 세션 기반 테스트 전용 Security 설정을 import합니다.

```java
@WebMvcTest(
    controllers = RecipeController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityConfig.class    // 실제 STATELESS 설정 제외
    )
)
@Import(RecipeControllerTest.TestSecurityConfig.class)
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

`sessionManagement`를 명시하지 않으면 Spring Security가 기본 세션 정책(IF_REQUIRED)을 씁니다. 이 상태에서 `authentication()` 포스트프로세서가 세션에 인증 정보를 저장하고, 테스트가 정상 동작합니다.

---

## 6. 삽질: Lombok 컴파일 에러

```gradle
// 팀 프로젝트 설정 — testCompileOnly가 없었음
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    // testCompileOnly, testAnnotationProcessor 없음!
}
```

`compileOnly`는 main 소스셋에만 적용됩니다. test 소스셋에서 `@Getter`/`@Setter` 등을 쓰면 컴파일 에러가 납니다.

올바른 해결 방법:
```gradle
testCompileOnly 'org.projectlombok:lombok'
testAnnotationProcessor 'org.projectlombok:lombok'
```

build.gradle을 건드리지 않기 위해 이번에는 테스트 클래스에서 Lombok을 쓰지 않고 수동으로 작성했습니다.

---

## 7. 실제 테스트 코드 — RecipeDraftServiceTest

```java
@ExtendWith(MockitoExtension.class)
class RecipeDraftServiceTest {

    @Mock RecipeRepository recipeRepository;
    @Mock IngredientMasterRepository ingredientMasterRepository;

    @InjectMocks RecipeDraftService recipeDraftService;

    @Test
    void createDraft_success() {
        // given
        RecipeDTO dto = RecipeDTO.builder().title("임시 레시피").build();
        UserEntity user = new UserEntity();
        user.setId(1);

        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setRecipeId(10L);
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
| Mockito (@Mock) | 서비스 레이어 로직 | DB 없이 격리된 테스트 |
| standaloneSetup | 예외처리, ControllerAdvice | Spring Context 불필요 |
| @WebMvcTest | 컨트롤러 + Security | HTTP 레이어 전체 테스트 |

테스트를 작성하면서 가장 크게 느낀 건, **서비스를 잘 분리해놓으면 테스트 작성도 훨씬 쉬워진다**는 것입니다. 챕터 5에서 RecipeService를 분리한 이유가 여기서 다시 확인됐습니다.

> 챕터 7 → 마무리 — 리팩토링 회고
