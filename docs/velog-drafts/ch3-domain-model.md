# Spring Boot 리팩토링 3 — 매직 넘버를 없애고, 레거시를 안전하게 처리하는 법

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

이번 챕터는 도메인 모델 개선입니다. 코드에 흩어진 `0`, `1`, `2` 같은 매직 넘버를 Enum으로 바꾸고, 당장 삭제할 수 없는 레거시 코드를 어떻게 처리했는지 다룹니다.

---

## 1. TradeStatus Enum

### 문제: 1이 뭔지 코드만 봐서는 알 수 없었다

```java
// 리팩토링 전
tradePost.setStatus(1);     // 1이 뭐지? 거래중? 완료?

if (tradePost.getStatus() == 0) {
    // 이 조건이 무엇을 의미하는지 바로 파악이 안 됨
}

if (post.getStatus() == 2) {
    throw new IllegalStateException("이미 완료된 거래입니다.");
}
```

DB에도 `status INT`로 저장되어 있어서, 0이 거래중인지 완료인지 다른 파일을 찾아봐야만 알 수 있었습니다.

### 해결: Java Enum 도입

```java
public enum TradeStatus {
    ONGOING,    // 거래중
    COMPLETED   // 거래완료
}
```

사용 코드가 이렇게 바뀝니다.

```java
// Before
tradePost.setStatus(1);
if (tradePost.getStatus() == 0) { ... }

// After
tradePost.setStatus(TradeStatus.ONGOING);
if (tradePost.getStatus() == TradeStatus.ONGOING) { ... }
```

코드 자체가 문서가 됩니다.

### Enum을 DB에 저장하는 두 가지 방법

JPA에서 Enum을 DB에 저장할 때 두 가지 방법이 있습니다.

**ORDINAL — 순서(숫자)로 저장**
```java
@Enumerated(EnumType.ORDINAL)
private TradeStatus status;
// ONGOING → 0, COMPLETED → 1
```

문제가 있습니다. Enum 중간에 `PENDING`을 추가하면 기존 `COMPLETED(1)`이 `PENDING(1)`이 됩니다. 기존 DB 데이터가 잘못 해석되는 것입니다.

**STRING — 이름으로 저장 (권장)**
```java
@Enumerated(EnumType.STRING)
private TradeStatus status;
// ONGOING → "ONGOING", COMPLETED → "COMPLETED"
```

DB에 문자열로 저장되어 순서 변경에 영향받지 않습니다. 이 방법을 사용했습니다.

### API 직렬화 결정: name() 기본값 유지

프론트엔드와 JSON으로 통신할 때 Enum이 어떻게 내려갈지 결정해야 합니다.

```json
// 기본 동작 (Jackson)
{ "status": "ONGOING" }   // name() — 기본값

// @JsonValue로 커스텀 값을 노출하면
{ "status": "거래중" }
```

이 프로젝트에서는 `name()` 기본값을 유지했습니다. 프론트엔드가 영어 코드값을 받아 한국어로 변환하는 방식이 유지보수에 더 유연하기 때문입니다.

---

## 2. Fridge @Deprecated 처리

### 배경: 왜 바로 삭제하지 않았나

Fridge는 사용자가 냉장고에 재료를 등록하는 기능입니다. 리팩토링 계획에서 `Fridge → Pantry`로 교체하는 큰 작업이 있었는데, 당장 삭제하면 문제가 생겼습니다.

1. Fridge API를 아직 사용 중인 프론트엔드가 깨진다
2. Fridge에 저장된 기존 사용자 데이터 처리 방법을 결정해야 한다
3. RecipeService가 Fridge를 의존하고 있어서 동시에 수정이 필요하다

범위가 너무 넓어지기 때문에 이번에는 `@Deprecated`로 표시하고, 새 기능(Pantry)과 공존하는 방식을 선택했습니다.

### @Deprecated의 의미

`@Deprecated`는 "이 코드는 더 이상 권장하지 않음 — 새 코드에서는 쓰지 마세요"를 알리는 어노테이션입니다.

```java
@Deprecated
@Entity
@Table(name = "fridge")
public class Fridge { ... }

@Deprecated
@RestController
@RequestMapping("/api/fridge")
public class FridgeController { ... }
```

효과는 세 가지입니다. IDE에서 취소선으로 경고 표시, 컴파일러 경고 발생, 코드를 읽는 사람에게 "대체제가 있다"는 신호.

### @Deprecated vs 즉시 삭제

| | @Deprecated | 즉시 삭제 |
|--|------------|---------|
| 기존 클라이언트 영향 | 없음 (하위 호환) | API 즉시 깨짐 |
| 마이그레이션 시간 | 제공 가능 | 없음 |
| 코드 복잡도 | 잠시 증가 | 감소 |
| 권장 시기 | 외부 API / 점진적 교체 | 내부 코드 정리 |

Javadoc과 함께 대체 방법을 명시하는 게 좋습니다.

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

리팩토링을 하면서 느낀 건, 코드를 삭제할 때도 순서와 이유가 있어야 한다는 것입니다. 무작정 지우면 다른 무언가가 깨집니다.

> 챕터 4 → 서비스 분리 — 단일 책임 원칙(SRP)
