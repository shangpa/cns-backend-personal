# Spring Boot 리팩토링 1 — 내 프로젝트 API를 RESTful하게 뜯어고친 이야기

안녕하세요, 백엔드 개발자를 꿈꾸는 shangpa입니다.

이번에 졸업 작품으로 완성한 프로젝트의 API를 리팩토링했습니다.
기능은 이미 다 동작하고 있었는데, swagger로 보니 불편하더라고요.

```
POST /joinUser
GET  /api/user/profile
POST /api/boards/{id}/like-toggle
GET  /api/fridge-history/all
POST /api/point/use
```

URL만 봐서는 뭘 하는 건지 한눈에 안 들어왔고,
`/update`, `/delete`, `/toggle`, `/all` 같은 동사들이 URL 곳곳에 박혀 있었습니다.

이게 왜 문제인지, 어떻게 바꿨는지 정리해보겠습니다.

---

## 1. 왜 바꿨나 — "그냥 돌아가는 API"의 문제점

처음엔 기능 구현을 위해 빠르게 만들었습니다.
REST 설계는 알고 있었지만 규칙에 맞게 만들지 않았습니다.
프론트에서 요청이 오면 데이터 넘겨주면 그만이었고, 잘 돌아가면 OK였습니다.

그런데 API가 쌓일수록 규칙이 없어지기 시작했습니다.

- 어디는 `POST /deleteXxx`, 또 어디는 `DELETE /xxx/{id}`
- 어떤 곳은 `/list`, `/all`, 어떤 곳은 그냥 컬렉션 경로
- `user`, `users` 혼용, `like`, `likes` 혼용

**API 목록만 봐서는 뭘 하는 건지 파악이 안 됐고**, 팀원과 소통할 때도 매번 코드를 직접 열어봐야 했습니다.

그래서 REST 원칙을 기준으로 전부 정리하기로 했습니다.

---

## 2. 핵심 원칙 — URL은 자원, 메서드는 행위

리팩토링의 기준은 딱 하나였습니다.
코드 없이 URL만 보고 이해할 수 있게 만들자.

> **URL은 명사(자원)로, 행위는 HTTP 메서드로 표현한다.**

| HTTP Method | 의미 | 예시 |
|-------------|------|------|
| GET | 조회 | `GET /api/users/me` |
| POST | 생성 | `POST /api/users` |
| PUT | 전체 수정 | `PUT /api/users/me/profile-image` |
| PATCH | 부분 수정 | `PATCH /api/users/me` |
| DELETE | 삭제 | `DELETE /api/fridges?name={name}` |

이 원칙 하나로 URL에 박혀있던 동사들을 전부 걷어낼 수 있었습니다.

---

## 3. 실제로 어떻게 바꿨나

### 사용자(User) & 인증(Auth)

가장 많이 쓰이는 도메인이라 기준을 잘 잡는 게 중요했습니다.

| 기능 | 변경 전 | 변경 후 |
|------|---------|---------|
| 회원 가입 | `POST /join` | `POST /api/users` |
| 사용자 정보 수정 | `PUT /api/user/update` | `PUT /api/users/me` |
| 프로필 이미지 업로드 | `POST /api/user/profile-image` | `PUT /api/users/me/profile-image` |
| 내 정보 조회 | `GET /api/user/profile` | `GET /api/users/me` |
| ID로 이름 조회 | `GET /api/user/profile-by-id` | `GET /api/users/{id}/name` |
| 유저명으로 ID 조회 | `GET /api/user/id` | `GET /api/users?username={username}` |
| 비밀번호 확인 | `POST /api/user/check-password` | `POST /api/user/auth/verify-password` |

**포인트 정리:**
- `update`, `profile` 같은 불필요한 단어 제거
- "내 정보"는 `/me`로 통일
- 인증 관련 기능은 `auth` 도메인으로 분리
- 검색 조건은 URL Path 대신 Query Parameter로

---

### 커뮤니티 (게시판, 댓글, 좋아요)

여기서 제일 많이 실수했던 부분이 **복수형 통일**이었습니다.
`like`인지 `likes`인지, `comment`인지 `comments`인지 혼용하고 있었거든요.

| 기능 | 변경 전 | 변경 후 |
|------|---------|---------|
| 게시글 좋아요 | `POST /api/boards/{id}/like` | `POST /api/boards/{id}/likes` |
| 게시글 댓글 작성 | `POST /api/boards/{id}/comment` | `POST /api/boards/{id}/comments` |
| 레시피 좋아요 토글 | `POST /api/recipes/{id}/like-toggle` | `POST /api/recipes/{id}/likes` |
| 좋아요한 레시피 목록 | `GET /api/recipes/like/list` | `GET /api/recipes/liked` |
| 중고거래 찜 토글 | `POST /api/tradeposts/{id}/save-toggle` | `POST /api/tradeposts/{id}/bookmarks` |
| 내가 찜한 거래 목록 | `GET /api/tradeposts/saved` | `GET /api/tradeposts/bookmarked` |

**포인트 정리:**
- 자원 이름은 **복수형**으로 통일 (`like` → `likes`)
- `toggle`, `save` 같은 동사 제거 → 실제 로직(토글 여부)은 서버 내부에서 처리
- "내가 찜한 목록" 같은 **내 소유의 데이터는 `/users/me/` 하위로** 이동

---

### 냉장고(Fridge) & 팬트리(Pantry)

이 도메인에서 가장 심각했던 건 `/use-ingredients`, `/delete-by-name` 같은 URL이었습니다.
URL에 행위가 너무 구체적으로 들어가 있어서, HTTP 메서드는 그냥 형식상 있는 것처럼 됐었죠.

| 기능 | 변경 전 | 변경 후 |
|------|---------|---------|
| 내 냉장고 조회 | `GET /api/fridges/my` | `GET /api/fridges/me` |
| 냉장고 재료 사용 | `POST /api/fridges/use-ingredients` | `PUT /api/fridges/ingredients` |
| 이름으로 재료 삭제 | `DELETE /api/fridges/delete-by-name` | `DELETE /api/fridges?name={name}` |
| 내 냉장고 내역 전체 조회 | `GET /api/fridge-history/all` | `GET /api/fridge-histories/me` |

**포인트 정리:**
- `/use-ingredients` → 재고를 수정하는 거니까 `PUT`이 맞다
- `/delete-by-name` → `DELETE` 메서드에 Query Parameter로 이름 전달
- `/all` 제거 → 컬렉션 경로(`/fridge-histories/me`)로 GET 요청 자체가 전체 조회 의미

---

### 채팅 & 알림 & 포인트

| 기능 | 변경 전 | 변경 후 |
|------|---------|---------|
| 채팅방 목록 | `GET /api/chat-room/list` | `GET /api/chat-rooms` |
| 메시지 조회 | `GET /api/chat-message` | `GET /api/chat-rooms/{id}/messages` |
| 모든 알림 삭제 | `DELETE /api/notifications/read/all` | `DELETE /api/notifications` |
| 포인트 사용 | `POST /api/point/use` | `POST /api/point/me/usage` |
| 포인트 조회 | `GET /api/point/my-point` | `GET /api/point/me/point` |
| 포인트 내역 | `GET /api/point/my-history` | `GET /api/point/me/history` |

**포인트 정리:**
- `/list`, `/all` 제거 → 컬렉션 경로에 GET 자체가 목록 의미
- 메시지는 특정 채팅방에 속한 자원 → `/chat-rooms/{id}/messages`로 계층 표현
- `use` 동사 → `usage` 명사로 변경

---

## 4. 리팩토링하면서 가장 많이 한 고민

### "/me" vs "/{id}"

"내 정보"를 다루는 API를 어떻게 표현할지 고민했습니다.

`/api/users/{id}`로 다 통일하는 방법도 있지만, 로그인된 사용자 본인의 정보를 다룰 땐 `/me`를 쓰는 게 훨씬 직관적이고, 토큰에서 사용자를 꺼내서 처리하기도 편합니다.

그래서 이렇게 기준을 잡았습니다:
- **본인 데이터 조회/수정** → `/api/users/me`
- **타인 데이터 조회** → `/api/users/{id}`

---

### 토글(Toggle) 처리를 어떻게 할 것인가

좋아요나 찜 같은 토글 기능이 좀 애매했습니다.

"이미 좋아요를 눌렀으면 취소, 아니면 추가" — 이 로직을 URL에 어떻게 담아야 하지?

결국 URL에서 toggle 표현을 없애고, **서버 내부에서 이미 존재하면 삭제, 없으면 추가**하는 방식으로 처리했습니다. 클라이언트는 그냥 `POST /api/recipes/{id}/likes` 하나만 알면 됩니다.

완벽한 정답은 아니지만, URL에 `toggle`이 박혀있는 것보다는 훨씬 낫다고 판단했습니다.

---

## 5. 마무리

이번 리팩토링을 하면서 가장 크게 느낀 건 이겁니다.

> **"좋은 API는 코드를 안 열어봐도 읽힌다."**

`GET /api/point/me/history` 이 URL 하나만 봐도 "내 포인트 기록 조회"라는 걸 바로 알 수 있습니다.

기능을 구현하는 것도 중요하지만, 그것을 어떻게 표현하느냐도 실력이라는 걸 이번에 제대로 배웠습니다.

아직 완벽한 REST는 아닐 수 있습니다.
하지만 **왜 이렇게 설계해야 하는가**를 한 번이라도 고민해본 것과 아닌 것은 분명히 다르다고 생각합니다.

> 챕터 2 → 보안 — 자격증명 분리 + 전역 예외처리
