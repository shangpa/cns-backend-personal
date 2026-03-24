# 챕터 3. Android 앱 API 연동 — 리팩토링으로 바뀐 엔드포인트 수정

> 백엔드를 리팩토링하면 API 경로가 바뀝니다. Android 쪽 코드도 맞춰 수정해야 합니다.
> 어떤 게 바뀌었는지 찾는 방법과 수정 내용을 정리했습니다.

---

## 1. BASE_URL 변경

로컬 개발 환경 IP에서 배포 서버 IP로 변경합니다.

`RetrofitInstance.kt`

```kotlin
// Before — 로컬 개발 환경
const val BASE_URL = "http://172.30.1.98:8080"

// After — OCI 배포 서버
const val BASE_URL = "http://138.2.117.37:8080"
```

---

## 2. 변경된 엔드포인트 6개

백엔드 리팩토링에서 RESTful 설계 원칙에 맞게 URL을 정리했습니다.
`ApiService.kt`에서 수정이 필요한 부분들입니다.

### 2-1. 좋아요 목록 조회

```kotlin
// Before
@GET("/api/recipes/like/list")

// After
@GET("/api/recipes/likes")
```

### 2-2. 좋아요 토글 (메인 피드)

```kotlin
// Before
@POST("/api/recipes/{recipeId}/like-toggle")

// After
@POST("/api/recipes/{recipeId}/likes")
```

### 2-3. 냉장고 재료 사용 처리

메서드도 POST → PUT으로 변경되었습니다.

```kotlin
// Before
@POST("api/fridges/use-ingredients")

// After
@PUT("api/fridges/ingredients")
```

### 2-4. 내 냉장고 조회

```kotlin
// Before
@GET("api/fridges/my")

// After
@GET("api/fridges/me")
```

### 2-5. 냉장고 재료 삭제 — 쿼리 파라미터명 변경

```kotlin
// Before
@DELETE("api/fridges/delete-by-name")
fun deleteFridgeByName(@Query("ingredientName") ingredientName: String, ...)

// After
@DELETE("api/fridges")
fun deleteFridgeByName(@Query("name") ingredientName: String, ...)
```

### 2-6. 좋아요 토글 (레시피 상세)

```kotlin
// Before
@POST("api/recipes/{recipeId}/like-toggle")

// After
@POST("api/recipes/{recipeId}/likes")
```

### 2-7. 냉장고 이력 전체 조회

```kotlin
// Before
@GET("/api/fridge-history/all")

// After
@GET("/api/fridge-history")
```

---

## 3. 변경 사항을 찾는 방법

리팩토링 커밋이 많을 때 "무엇이 바뀌었나"를 빠르게 파악하는 방법들입니다.

### 방법 1: git log로 컨트롤러 변경 커밋만 필터링

```bash
git log --oneline --all | grep -i "controller\|endpoint\|api\|route"
```

### 방법 2: 컨트롤러 파일 변경 이력 확인

```bash
git log --oneline -- "src/main/java/**/controller/**"
git diff HEAD~10 HEAD -- "src/main/java/**/controller/**"
```

### 방법 3: Swagger UI 활용

배포 후 `http://<서버IP>:8080/swagger-ui/index.html`에 접속하면
현재 서버의 실제 엔드포인트 목록을 확인할 수 있습니다.

Android 쪽 ApiService.kt와 Swagger 목록을 나란히 놓고 비교하면
불일치를 빠르게 찾을 수 있습니다.

---

## 4. AuthInterceptor — JWT 토큰 자동 첨부

로그인 후 발급된 JWT를 모든 요청 헤더에 자동으로 넣어주는 인터셉터입니다.

```kotlin
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = // SharedPreferences 또는 DataStore에서 토큰 읽기

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
```

---

## 5. WebSocket 연결 (채팅)

STOMP over WebSocket 연결 시 JWT를 쿼리 파라미터로 전달합니다.

```kotlin
val wsUrl = "ws://138.2.117.37:8080/ws/chat?token=$jwtToken"
```

백엔드 WebSocket 핸드셰이크 인터셉터에서 `token` 파라미터를 읽어 인증을 처리합니다.
