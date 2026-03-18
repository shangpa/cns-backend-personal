# 챕터 3 개념 정리 — Enum, @Deprecated, JSON 직렬화

---

## [개념 1] Java Enum의 특성

Enum은 단순히 상수 목록이 아닙니다.

```java
public enum TradeStatus {
    ONGOING,
    COMPLETED
}
```

**Enum은 클래스다.**
- 각 값(ONGOING, COMPLETED)은 `TradeStatus` 타입의 싱글톤 인스턴스
- 메서드, 필드, 생성자를 가질 수 있음
- `implements` 인터페이스 가능 (상속은 불가 — 이미 `java.lang.Enum`을 상속)

**기본 제공 메서드:**
```java
TradeStatus.ONGOING.name()      // "ONGOING" — 선언한 이름 그대로
TradeStatus.ONGOING.ordinal()   // 0 — 선언 순서 (0부터 시작)

TradeStatus.valueOf("ONGOING")  // 이름으로 enum 값 얻기
TradeStatus.values()            // TradeStatus[] — 모든 값 배열
```

**Enum 비교는 `==` 사용 (equals()도 동작하지만 관례상 ==):**
```java
if (post.getStatus() == TradeStatus.ONGOING) { ... }   // 권장
if (post.getStatus().equals(TradeStatus.ONGOING)) { ... }  // 동작은 하지만 불필요
```
Enum 인스턴스는 싱글톤이라 `==`로 동일성 비교가 가능합니다.

---

## [개념 2] `@Enumerated(EnumType.ORDINAL)` vs `@Enumerated(EnumType.STRING)`

JPA가 Enum을 DB에 저장할 때 두 가지 방법이 있습니다.

**ORDINAL — 숫자(순서)로 저장**
```java
@Enumerated(EnumType.ORDINAL)
private TradeStatus status;
```
```
ONGOING   → 0
COMPLETED → 1
```

**문제 상황:**
```java
// 나중에 PENDING을 추가한다면?
public enum TradeStatus {
    PENDING,    // 새로 추가 → 0번 자리 차지
    ONGOING,    // 기존 0 → 이제 1
    COMPLETED   // 기존 1 → 이제 2
}
```
DB에는 여전히 `0`으로 저장된 기존 데이터가 있지만, 이제 `0`은 `PENDING`입니다. 데이터가 오염됩니다.

**STRING — 이름으로 저장 (권장)**
```java
@Enumerated(EnumType.STRING)
private TradeStatus status;
```
```
ONGOING   → "ONGOING"
COMPLETED → "COMPLETED"
```
Enum 순서가 바뀌어도 문자열 값은 변하지 않으므로 안전합니다. 저장 공간이 조금 더 들지만 무시할 수준입니다.

---

## [개념 3] Jackson JSON 직렬화/역직렬화

**직렬화(Serialization):** Java 객체 → JSON 문자열 (응답 시)
**역직렬화(Deserialization):** JSON 문자열 → Java 객체 (요청 시)

Spring Boot는 기본으로 Jackson(`ObjectMapper`)을 사용합니다.

**Enum의 기본 Jackson 동작:**
```java
// 응답 JSON
{ "status": "ONGOING" }   // 기본: name() 값 출력
```

---

## [개념 4] `@JsonValue` — 직렬화 커스터마이징

`@JsonValue`를 붙인 메서드의 반환값이 JSON으로 출력됩니다.

```java
public enum TradeStatus {
    ONGOING("거래중"),
    COMPLETED("거래완료");

    private final String label;

    TradeStatus(String label) { this.label = label; }

    @JsonValue
    public String getLabel() { return label; }
}
```

```json
{ "status": "거래중" }   // @JsonValue 적용 시
```

**주의:** `@JsonValue`를 적용하면 역직렬화(`"거래중"` → `TradeStatus.ONGOING`)도 자동으로 처리될 것 같지만, 정확히는 `@JsonCreator`를 따로 작성해야 합니다.

---

## [개념 5] `@JsonCreator` — 역직렬화 커스터마이징

JSON → Java 객체 변환 시 사용할 팩토리 메서드를 지정합니다.

```java
public enum TradeStatus {
    ONGOING("거래중"),
    COMPLETED("거래완료");

    private final String label;
    TradeStatus(String label) { this.label = label; }

    @JsonValue
    public String getLabel() { return label; }

    @JsonCreator
    public static TradeStatus from(String value) {
        for (TradeStatus s : values()) {
            // "거래중"으로도, "ONGOING"으로도 받을 수 있게
            if (s.label.equals(value) || s.name().equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
```

```
요청 JSON: { "status": "거래중" }
    ↓ @JsonCreator from("거래중") 호출
    ↓ TradeStatus.ONGOING 반환
```

---

## [개념 6] `@Deprecated` — Java와 Javadoc 두 가지 방법

**Java 어노테이션:**
```java
@Deprecated(since = "2.0", forRemoval = true)
public class FridgeService { ... }
```
- `since` — 어느 버전부터 deprecated인지 명시 (문서 목적)
- `forRemoval = true` — 향후 버전에서 완전히 제거 예정임을 명시. IDE가 더 강한 경고 표시.

**Javadoc 태그 (소문자, 설명 포함):**
```java
/**
 * @deprecated Pantry로 대체됨.
 *             {@link PantryService}를 사용하세요.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class FridgeService { ... }
```

Javadoc `@deprecated`(소문자)는 "왜 deprecated인지, 대체제가 무엇인지"를 설명하는 문서 태그입니다.
`@Deprecated`(대문자) 어노테이션과 항상 함께 쓰는 것이 관례입니다.

**IDE 표시:**
```java
FridgeService fridge = new FridgeService();
//            ^^^^^^^^^^^^^ 취소선으로 표시됨
```

**`forRemoval = true` 효과:**
```
@Deprecated              → IDE 경고 (노란색)
@Deprecated(forRemoval=true) → IDE 더 강한 경고 (빨간색 취소선)
                             → 컴파일러 경고 레벨 업그레이드
```

---

## [개념 7] 매직 넘버(Magic Number) 문제

**매직 넘버:** 의미 없어 보이는 숫자/문자열 리터럴이 코드에 직접 사용되는 것.

```java
// 매직 넘버 — 1이 무엇인지 주석 없이는 알 수 없음
if (post.getStatus() == 1) { ... }
post.setStatus(0);

// Enum 사용 후 — 코드 자체가 의도를 설명
if (post.getStatus() == TradeStatus.ONGOING) { ... }
post.setStatus(TradeStatus.COMPLETED);
```

**매직 넘버가 왜 나쁜가:**
1. 가독성 저하 — 1이 무엇인지 바로 알 수 없음
2. 중복 — 여러 곳에 `== 1`이 흩어지면 나중에 의미가 바뀔 때 모두 찾아 바꿔야 함
3. 오타 위험 — `== 2`를 `== 3`으로 잘못 쓰면 컴파일러가 못 잡음. Enum은 존재하지 않는 값 사용 시 컴파일 에러.
