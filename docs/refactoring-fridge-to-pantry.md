# 냉장고 도메인 리팩토링: Fridge → Pantry + IngredientMaster 설계

## 요약

레시피 재료와 냉장고 재료를 연동하는 기능을 구현하는 과정에서,
**재료명 불일치 문제**를 해결하기 위해 냉장고 도메인 전체를 재설계했습니다.

---

## 문제 상황 (AS-IS)

기존 `fridge` 패키지는 재료를 **단순 문자열(String)** 로 저장했습니다.

```java
// 기존 방식
private String ingredientName; // "계란", "달걀", "달걀(대)", "egg" 모두 다른 재료로 인식
```

이 구조에서는 다음과 같은 문제가 발생했습니다:

- 사용자마다 같은 재료를 다르게 입력 ("계란" vs "달걀")
- 레시피에 등록된 재료명과 냉장고 재료명이 일치하지 않음
- **"레시피 완성 → 사용한 재료 냉장고에서 자동 차감"** 기능 구현 불가

---

## 해결 방법 (TO-BE)

### 1. IngredientMaster 테이블 도입

재료를 표준화된 마스터 테이블로 관리합니다.

```java
@Entity
@Table(name = "ingredient_master")
public class IngredientMaster {
    private Long id;
    private String nameKo;          // 표준 재료명 (예: "달걀")
    private IngredientCategory category;  // 카테고리 (육류, 채소 등)
    private UnitEntity defaultUnit; // 기본 단위 (개, g, ml 등)
    private String iconUrl;         // 재료 아이콘
}
```

- 재료명에 **UNIQUE 제약조건** 적용 → 중복 등록 방지
- 모든 재료는 반드시 IngredientMaster를 참조 → 동의어 문제 근본 해결

### 2. 앱 시작 시 재료명 캐싱

매 요청마다 DB를 조회하지 않도록, 서버 시작 시 전체 재료명을 메모리에 캐싱합니다.

```java
@PostConstruct
public void init() {
    List<String> allNames = ingredientMasterRepository.findAll()
            .stream().map(IngredientMaster::getNameKo).toList();
    ingredientNameCache.initialize(allNames);
}
```

### 3. Vision API 연동으로 재료 자동 인식 및 매핑

카메라로 식재료를 촬영하면 Google Cloud Vision API가 재료를 감지하고,
감지된 이름을 IngredientMaster에 자동 매핑합니다.

```
카메라 촬영 → Vision API 라벨 감지 → IngredientMaster 매핑 → Pantry에 재료 추가
```

### 4. Pantry 패키지로 냉장고 도메인 재설계

`fridge` → `pantry`로 완전히 교체하며 IngredientMaster 기반 구조로 전환했습니다.

| | Fridge (레거시) | Pantry (현재) |
|---|---|---|
| 재료 저장 방식 | String (자유 입력) | IngredientMaster 참조 |
| 레시피 연동 | 불가 | 가능 |
| 재료 차감 기능 | 불가 | 가능 |
| OCR 연동 | 없음 | 있음 |
| 히스토리 추적 | 없음 | 있음 |

---

## 핵심 기능 흐름

```
[레시피 등록]
    ↓
레시피 재료 → IngredientMaster ID로 저장
    ↓
[레시피 완성 처리]
    ↓
레시피 재료 목록 조회 → Pantry에서 동일 IngredientMaster ID 재료 차감
```

---

## 기술적 의사결정 포인트

### Q. 왜 String이 아닌 IngredientMaster를 참조하게 했나요?
한국어에서는 같은 식재료를 다양한 이름으로 부릅니다 ("계란"/"달걀", "대파"/"파").
String 비교로는 동의어를 처리할 수 없어, **마스터 테이블을 공통 기준점**으로 삼는 방식을 선택했습니다.
이는 데이터 정규화(Normalization) 원칙과도 일치합니다.

### Q. 기존 Fridge는 왜 삭제하지 않고 @Deprecated 처리했나요?
기존 Fridge 엔티티를 즉시 제거할 경우, 이미 배포된 구버전 앱에서 해당 API를 호출하는 클라이언트가 런타임 오류를 겪을 수 있고, 
기존에 쌓인 데이터에 대한 마이그레이션 전략도 부재한 상태였습니다. 때문에 삭제 대신 @Deprecated를 명시해 
"이 코드는 레거시이며 더 이상 신규 개발에 사용해선 안 된다"는 의도를 전달했습니다. 
이를 통해 신규 코드가 Pantry 기반으로 작성되도록 유도할 수 있었습니다.
---

## 배운 점

- **도메인 문제를 DB 설계로 해결**: 단순 String 저장의 한계를 마스터 테이블 도입으로 극복
- **기능 확장성 확보**: IngredientMaster 기반으로 OCR 인식, 레시피 연동, 자동 차감 등 다양한 기능 연결 가능
- **점진적 마이그레이션**:즉시 삭제하지 않고 @Deprecated로 안전하게 전환
