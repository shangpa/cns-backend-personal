# 챕터 4. 서비스 분리 — 단일 책임 원칙(SRP)

> 커버하는 커밋: `RecipeDraftService 분리`, `RecipeStatService 분리`

---

## 4-1. 단일 책임 원칙(SRP)이란

> "클래스가 바뀌는 이유는 하나여야 한다." — Robert C. Martin

SRP(Single Responsibility Principle)는 SOLID 원칙 중 첫 번째입니다.
**"바뀌는 이유가 하나"**라는 게 핵심인데, 좀 더 실용적으로 표현하면:

> "이 클래스를 수정해야 하는 이유가 여러 가지라면, 클래스를 분리하라."

---

## 4-2. RecipeService의 21개 메서드

리팩토링 전 `RecipeService`에는 21개 메서드가 있었습니다.

```
RecipeService (21개)
│
├── [발행 레시피 CRUD]
│   ├── getAllRecipes()
│   ├── getAllPublicRecipes()
│   ├── getRecipeById()
│   ├── createRecipe()
│   ├── updateRecipe()
│   └── deleteRecipe()
│
├── [초안(Draft) 관리]
│   ├── createDraft()
│   ├── getMyDraftById()
│   ├── updateDraft()
│   ├── deleteDraft()
│   └── publishDraft()
│
├── [통계(Stat) 조회]
│   ├── getCategoryStats()
│   ├── getMonthlyCategoryStatsByName()
│   ├── getRecentFourMonthsStats()
│   ├── countRecipeMonthly()
│   ├── sumRecipeViewsMonthly()
│   └── getRecipeStats()
│
└── [추천 / 검색 / 기타]
    ├── searchRecipesByTitle()
    ├── suggestByType()
    ├── getExpectedIngredients()
    └── deleteRecipeByAdmin()
```

**"바뀌는 이유"를 물어봤습니다:**

- 초안 저장 로직이 바뀌면? → RecipeService 수정
- 통계 집계 방식이 바뀌면? → RecipeService 수정
- 발행 레시피 생성 흐름이 바뀌면? → RecipeService 수정

3가지 다른 이유로 바뀌므로 SRP 위반입니다.

---

## 4-3. RecipeDraftService 분리

초안 관련 5개 메서드를 `RecipeDraftService`로 분리했습니다.

**분리 전 (RecipeService 내부):**
```java
@Service
public class RecipeService {
    // 발행 레시피 메서드들...

    public Long createDraft(RecipeDTO dto, UserEntity user) { ... }
    public RecipeDTO getMyDraftById(Long id, UserEntity user) { ... }
    public Recipe updateDraft(Long id, RecipeDTO dto, int userId) { ... }
    public void deleteDraft(Long id, int userId) { ... }
    public Recipe publishDraft(Long id, boolean isPublic, int userId) { ... }
}
```

**분리 후 (독립 클래스):**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeDraftService {

    private final RecipeRepository recipeRepository;
    private final IngredientMasterRepository ingredientMasterRepository;

    @Transactional
    public Long createDraftTransactional(RecipeDTO dto, UserEntity user) {
        Recipe entity = dto.toEntityDraftSafe();
        entity.setUser(user);
        entity.setDraft(true);
        entity.setPublic(false);
        entity.setCreatedAt(LocalDateTime.now());

        recipeRepository.save(entity);

        // 재료 연결 처리...

        return entity.getRecipeId();
    }

    @Transactional(readOnly = true)
    public RecipeDTO getMyDraftById(Long recipeId, UserEntity user) { ... }

    @Transactional
    public Recipe updateDraft(Long id, RecipeDTO dto, int userId) { ... }

    @Transactional
    public void deleteDraft(Long id, int userId) { ... }

    @Transactional
    public Recipe publishDraft(Long id, boolean isPublic, int userId) { ... }
}
```

**RecipeService와 의존 관계:**
```
RecipeService ──────────────── 발행 레시피 CRUD
                               (RecipeRepository 의존)

RecipeDraftService ──────────── 초안 관리
                               (RecipeRepository + IngredientMasterRepository 의존)
```

두 서비스는 서로 의존하지 않습니다. RecipeRepository를 각자 가집니다.

---

## 4-4. RecipeStatService 분리

통계 관련 6개 메서드를 `RecipeStatService`로 분리했습니다.

```java
@Service
@RequiredArgsConstructor
public class RecipeStatService {

    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getCategoryStats() { ... }

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getMonthlyCategoryStatsByName(String category) { ... }

    @Transactional(readOnly = true)
    public List<RecipeMonthlyStatsDTO> getRecentFourMonthsStats() { ... }

    @Transactional(readOnly = true)
    public List<BoardMonthlyStatsDTO> countRecipeMonthly(LocalDateTime startDate) { ... }

    @Transactional(readOnly = true)
    public List<BoardMonthlyStatsDTO> sumRecipeViewsMonthly(LocalDateTime startDate) { ... }

    @Transactional(readOnly = true)
    public List<RecipeStatDTO> getRecipeStats(StatType type, LocalDate startDate,
                                              LocalDate endDate, Integer year, Integer month) { ... }
}
```

**특징:** 모든 메서드가 `readOnly = true`입니다. 통계 조회는 데이터를 변경하지 않기 때문입니다.

---

## 4-5. 컨트롤러 의존 주입 변경

서비스를 분리하면 컨트롤러의 주입도 바뀝니다.

**RecipeController — Before:**
```java
@RestController
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;   // 모든 것이 여기에

    @PostMapping("/drafts")
    public ResponseEntity<?> createDraft(...) {
        Long id = recipeService.createDraft(dto, user);  // RecipeService에서 처리
        ...
    }
}
```

**RecipeController — After:**
```java
@RestController
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;
    private final RecipeDraftService recipeDraftService;   // 추가

    @PostMapping("/drafts")
    public ResponseEntity<?> createDraft(...) {
        Long id = recipeDraftService.createDraftTransactional(dto, user);   // 전용 서비스
        ...
    }
}
```

**AdminController — Before:**
```java
@RestController
@RequiredArgsConstructor
public class AdminController {
    private final RecipeService recipeService;   // 통계도 RecipeService에서
}
```

**AdminController — After:**
```java
@RestController
@RequiredArgsConstructor
public class AdminController {
    private final RecipeService recipeService;
    private final RecipeStatService recipeStatService;   // 추가

    @GetMapping("/stats/recipes/category")
    public ResponseEntity<?> getCategoryStats() {
        return ResponseEntity.ok(recipeStatService.getCategoryStats());
    }
}
```

---

## 4-6. RECOMMEND 그룹은 왜 이번에 분리 안 했나

```
추천 / 검색 / 기타 그룹:
- searchRecipesByTitle()       → 검색
- suggestByType()              → 추천
- getExpectedIngredients()     → 재료 매칭
- deleteRecipeByAdmin()        → 관리자 기능
```

이 그룹도 "여러 이유로 바뀌는" 문제가 있습니다.
`RecipeSearchService`와 `RecommendService`로 분리가 이상적입니다.

**이번에 하지 않은 이유:**
- 검색 관련은 `RecipeSearchService`가 이미 별도 패키지에 존재했음
- 재료 매칭 로직은 Pantry 리팩토링과 연계되어 있어 함께 수정해야 함
- 범위를 제한하지 않으면 리팩토링이 끝나지 않음

리팩토링의 현실적인 교훈: **완벽하게 하려다 아무것도 못 하는 것보다, 명확히 범위를 정하고 하나씩 개선하는 게 낫습니다.**

---

## 분리 전후 클래스 다이어그램

```
Before:
RecipeController ──→ RecipeService (21개 메서드)
AdminController  ──→ RecipeService

After:
RecipeController ──→ RecipeService      (발행 레시피 CRUD + 기타)
                 ──→ RecipeDraftService (초안 관리 5개)
AdminController  ──→ RecipeService
                 ──→ RecipeStatService  (통계 조회 6개)
```

---

## 정리

| 변경 전 | 변경 후 |
|---------|---------|
| RecipeService 21개 메서드 | RecipeService + RecipeDraftService + RecipeStatService |
| 한 클래스가 여러 이유로 바뀜 | 각 클래스가 하나의 이유로만 바뀜 |
| 초안 테스트가 통계 코드 영향 받음 | 독립적인 단위 테스트 가능 |

> 다음: [챕터 5 → 테스트 코드 — 처음 작성하면서 배운 것들]
