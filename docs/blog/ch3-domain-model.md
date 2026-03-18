# 챕터 3. 도메인 모델 개선 — Enum과 레거시 처리

> 커버하는 커밋: `TradeStatus Enum`, `Fridge @Deprecated`

---

## 3-1. TradeStatus Enum

### 문제: 매직 넘버가 코드 전체에 흩어져 있었다

```java
// 리팩토링 전 — 0, 1, 2가 무엇을 의미하는지 코드만 봐서는 알 수 없음
tradePost.setStatus(1);     // 1이 뭐지? 거래중? 완료?

if (tradePost.getStatus() == 0) {
    // 조건이 무엇인지 바로 파악 안 됨
}

if (post.getStatus() == 2) {
    throw new IllegalStateException("이미 완료된 거래입니다.");
}
```

DB에도 `status INT`로 저장되어 있었는데, 0이 `거래중`인지 `거래완료`인지 DB 스키마나 다른 코드를 찾아봐야 알 수 있었습니다.

### 해결: Java Enum 도입

```java
public enum TradeStatus {
    ONGOING,    // 거래중
    COMPLETED   // 거래완료
}
```

사용 코드가 바뀝니다:

```java
// Before
tradePost.setStatus(1);
if (tradePost.getStatus() == 0) { ... }

// After
tradePost.setStatus(TradeStatus.ONGOING);
if (tradePost.getStatus() == TradeStatus.ONGOING) { ... }
```

코드 자체가 문서가 됩니다.

### Enum과 DB 저장 방법

JPA에서 Enum을 DB에 저장할 때 두 가지 방법이 있습니다.

**`@Enumerated(EnumType.ORDINAL)` — 순서(숫자)로 저장**
```java
@Enumerated(EnumType.ORDINAL)
private TradeStatus status;
// ONGOING → 0, COMPLETED → 1
```
문제: Enum 순서가 바뀌면 기존 DB 데이터가 잘못 해석됩니다.
`PENDING`을 중간에 추가하면 기존 `COMPLETED(1)`이 `PENDING(1)`이 됩니다.

**`@Enumerated(EnumType.STRING)` — 이름으로 저장 (권장)**
```java
@Enumerated(EnumType.STRING)
private TradeStatus status;
// ONGOING → "ONGOING", COMPLETED → "COMPLETED"
```
DB에 문자열로 저장되어 순서 변경에 영향받지 않습니다.

### API 직렬화: `name()` vs `ordinal()` vs `@JsonValue`

프론트엔드와 JSON으로 통신할 때 Enum이 어떻게 직렬화될지 결정해야 합니다.

**기본 동작 (Jackson)**
```json
{ "status": "ONGOING" }   // name() — 기본값
{ "status": 0 }           // ordinal() — @JsonFormat(shape=NUMBER) 설정 시
```

**`@JsonValue`로 커스텀 값 노출**
```java
public enum TradeStatus {
    ONGOING("거래중"),
    COMPLETED("거래완료");

    private final String displayName;

    TradeStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TradeStatus from(String value) {
        for (TradeStatus s : values()) {
            if (s.displayName.equals(value) || s.name().equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
```

```json
{ "status": "거래중" }   // @JsonValue 적용 시
```

이 프로젝트에서는 `@JsonValue`를 사용하지 않고 `name()` 기본값을 유지했습니다.
프론트엔드가 영어 코드값을 받아 한국어로 변환하는 방식이 유지보수에 더 유연하기 때문입니다.

---

## 3-2. Fridge `@Deprecated` 처리

### 배경: Fridge를 바로 삭제하지 않은 이유

Fridge는 사용자가 냉장고에 재료를 등록하는 기능입니다.
리팩토링 계획에서 `Fridge → Pantry`로 교체하는 큰 작업이 있었습니다.
(Pantry는 IngredientMaster 기반으로 재료를 표준화한 버전)

그런데 Fridge를 당장 삭제하면:
1. Fridge API를 아직 사용 중인 프론트엔드가 깨짐
2. Fridge에 저장된 기존 사용자 데이터 처리 방법을 결정해야 함
3. RecipeService가 Fridge를 의존하고 있어서 동시에 수정이 필요

범위가 너무 넓어지기 때문에 이번 리팩토링에서는 `@Deprecated`로 표시하고 새 기능(Pantry)과 공존하는 방식을 선택했습니다.

### `@Deprecated`의 의미

`@Deprecated`는 "이 코드는 더 이상 권장하지 않음 — 새 코드에서는 쓰지 마세요"를 알리는 어노테이션입니다.

```java
@Deprecated
@Entity
@Table(name = "fridge")
public class Fridge {
    // ...
}

@Deprecated
@RestController
@RequestMapping("/api/fridge")
public class FridgeController {
    // ...
}
```

**효과:**
- IDE에서 사용 시 취소선으로 경고 표시
- 컴파일러 경고 발생 (`-Xlint:deprecation` 옵션 시)
- 코드를 읽는 사람에게 "대체제가 있다"는 신호

**`@Deprecated` vs 코드 삭제:**

| | @Deprecated | 즉시 삭제 |
|--|------------|---------|
| 기존 클라이언트 영향 | 없음 (하위 호환) | API 즉시 깨짐 |
| 마이그레이션 시간 | 제공 가능 | 없음 |
| 코드 복잡도 | 잠시 증가 | 감소 |
| 권장 시기 | 외부 API / 점진적 교체 | 내부 코드 정리 |

### 실제 `@Deprecated` 활용 패턴

Javadoc과 함께 대체 방법을 명시하는 것이 좋습니다:

```java
/**
 * @deprecated Pantry로 대체됨. {@link PantryService}를 사용하세요.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class FridgeService { ... }
```

`forRemoval = true`는 "향후 버전에서 완전히 제거할 예정"을 명시합니다.

---

## 정리

| 변경 전 | 변경 후 |
|---------|---------|
| `status == 1` 매직 넘버 | `TradeStatus.ONGOING` Enum |
| DB에 INT로 저장 | `@Enumerated(STRING)` 문자열 저장 |
| Fridge 코드 그냥 존재 | `@Deprecated` 마킹 + Pantry 병행 |

> 다음: [챕터 4 → 서비스 분리 — 단일 책임 원칙(SRP)]
