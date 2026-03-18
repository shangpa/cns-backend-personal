# 챕터 4 개념 정리 — 단일 책임 원칙(SRP), 서비스 설계

---

## [개념 1] SOLID 원칙 전체 맥락

SRP는 SOLID 5원칙 중 첫 번째입니다. 나머지도 알아두면 좋습니다.

| 원칙 | 이름 | 한 줄 요약 |
|------|------|---------|
| **S** | Single Responsibility | 클래스는 하나의 이유로만 바뀌어야 한다 |
| **O** | Open/Closed | 확장에는 열려 있고, 수정에는 닫혀 있어야 한다 |
| **L** | Liskov Substitution | 하위 타입은 상위 타입을 대체할 수 있어야 한다 |
| **I** | Interface Segregation | 클라이언트가 사용하지 않는 인터페이스에 의존하면 안 된다 |
| **D** | Dependency Inversion | 구체 클래스가 아닌 추상화에 의존해야 한다 |

이번 챕터에서 적용한 것은 **S(SRP)**입니다.

---

## [개념 2] SRP — "바뀌는 이유" 가 핵심

단일 책임 원칙을 "하나의 일만 해야 한다"로 이해하면 애매합니다.
정확한 정의는:

> **"클래스가 바뀌는 이유(이해관계자)가 하나여야 한다."**

**실용적 질문 방법:**
```
RecipeService를 수정해야 하는 경우가 무엇인가?

1. 레시피 발행 로직이 바뀔 때       → RecipeService 수정
2. 초안 저장 방식이 바뀔 때         → RecipeService 수정
3. 통계 집계 로직이 바뀔 때         → RecipeService 수정

→ 3가지 이유가 있음 = SRP 위반 → 3개로 분리
```

**분리 후:**
```
RecipeService     — 발행 레시피 로직이 바뀔 때만 수정
RecipeDraftService — 초안 로직이 바뀔 때만 수정
RecipeStatService  — 통계 로직이 바뀔 때만 수정
```

---

## [개념 3] `@RequiredArgsConstructor` 와 생성자 주입

Lombok `@RequiredArgsConstructor`는 `final` 필드를 파라미터로 받는 생성자를 자동 생성합니다.

```java
@Service
@RequiredArgsConstructor
public class RecipeDraftService {

    private final RecipeRepository recipeRepository;           // final
    private final IngredientMasterRepository ingredientMasterRepository;  // final
}
```

생성된 코드:
```java
public RecipeDraftService(RecipeRepository recipeRepository,
                          IngredientMasterRepository ingredientMasterRepository) {
    this.recipeRepository = recipeRepository;
    this.ingredientMasterRepository = ingredientMasterRepository;
}
```

**왜 생성자 주입이 권장되는가:**

| | 필드 주입 (@Autowired) | 생성자 주입 |
|--|----------------------|-----------|
| 불변성 | 불가 (final 못 씀) | 가능 (final) |
| 테스트 | Mock 주입 어려움 | 생성자로 직접 주입 가능 |
| 순환 의존 감지 | 런타임 | 컴파일/시작 시점 |
| Spring 권장 | 아님 | 권장 |

```java
// 필드 주입 — Spring 없이 테스트하기 어려움
@Autowired
private RecipeRepository recipeRepository;

// 생성자 주입 — 테스트에서 직접 생성 가능
RecipeDraftService service = new RecipeDraftService(
    mockRecipeRepository,
    mockIngredientMasterRepository
);
```

---

## [개념 4] Spring 서비스 간 의존 관계 설계

**서비스 A가 서비스 B를 의존해도 되는가?**

기술적으로 가능하지만, 주의해야 할 점이 있습니다.

```java
@Service
public class RecipeService {
    private final RecipeDraftService recipeDraftService;  // 서비스가 서비스를 의존
}
```

**순환 의존(Circular Dependency) 문제:**
```
RecipeService → RecipeDraftService → RecipeService (다시 돌아옴 — 문제!)
```
Spring이 Bean을 만들려고 할 때, A가 B를 필요로 하는데 B도 A를 필요로 하면 생성 자체를 못 합니다.
Spring 6+에서는 기본적으로 순환 의존을 허용하지 않습니다.

**이 프로젝트의 설계:**
```
RecipeService      → RecipeRepository (Repository만 의존)
RecipeDraftService → RecipeRepository + IngredientMasterRepository
RecipeStatService  → RecipeRepository
```
세 서비스는 서로를 의존하지 않고 각자 Repository만 의존합니다.
공통 로직이 필요하면 Repository 레이어에 두거나 별도 유틸 클래스를 만듭니다.

---

## [개념 5] 왜 AdminController가 RecipeStatService를 직접 쓰는가

```java
@RestController
public class AdminController {
    private final RecipeStatService recipeStatService;  // 직접 주입

    @GetMapping("/admin/stats/recipes")
    public ResponseEntity<?> getStats(...) {
        return ResponseEntity.ok(recipeStatService.getCategoryStats());
    }
}
```

**왜 RecipeService를 거치지 않는가?**

`RecipeService`가 `RecipeStatService`를 감싸서 위임하는 구조도 가능하지만:
```java
// RecipeService가 위임하는 방식 — 불필요한 간접 계층
public class RecipeService {
    private final RecipeStatService recipeStatService;
    public List<RecipeStatDTO> getCategoryStats() {
        return recipeStatService.getCategoryStats();  // 그냥 위임만 함
    }
}
```

이런 위임 메서드는 아무 로직도 없이 그냥 전달만 합니다.
컨트롤러가 직접 RecipeStatService를 쓰는 게 더 명확하고 불필요한 간접 계층을 없앱니다.

**Controller → Service 의존의 실용적 원칙:**
- 컨트롤러가 여러 서비스를 주입받아도 괜찮습니다.
- 단, 컨트롤러가 비즈니스 로직을 직접 갖지 않아야 합니다 (라우팅과 권한 검사만).

---

## [개념 6] 클래스 다이어그램 읽는 법 (간단)

```
RecipeController ──→ RecipeService
```
`──→` 는 "의존한다 (사용한다)"는 의미.
`RecipeController`가 `RecipeService`를 필드로 가짐.

```
RecipeDraftService ──→ RecipeRepository
RecipeStatService  ──→ RecipeRepository
```
두 서비스가 같은 Repository를 의존해도 됩니다.
Spring은 Repository를 싱글톤 Bean으로 관리하므로 하나의 인스턴스를 공유합니다.
