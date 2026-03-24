# 챕터 5. 마치며 — 배운 점과 남은 작업

> 리팩토링한 코드를 실제로 배포하고 앱과 연결해보니 보이지 않던 것들이 보였습니다.

---

## 이번 작업에서 배운 것

### 1. 코드를 바꾸면 연동된 모든 곳을 함께 확인해야 한다

백엔드 리팩토링을 하면서 RESTful하게 URL을 정리했는데,
Android 쪽 `ApiService.kt`에 하드코딩된 경로들이 7곳이나 맞지 않았습니다.

API 스펙이 바뀌면 소비자(클라이언트)에게도 변경이 전파됩니다.
팀 프로젝트라면 API 변경 시 반드시 공지하거나, OpenAPI 스펙 파일로 계약을 명확히 해야 합니다.

### 2. 보안 설정은 경로가 바뀔 때마다 재검토해야 한다

`/api/user/auth/join` 회원가입이 403으로 막힌 이유는
SecurityConfig의 permitAll 목록에 새 경로를 추가하지 않았기 때문입니다.

새 URL 패턴을 추가할 때마다 "인증이 필요한가/불필요한가"를 명시적으로 결정하는 습관이 필요합니다.

### 3. 환경 변수는 배포 전에 검증하라

JWT_SECRET이 너무 짧아서 500 에러가 났습니다.
로컬 `.env`에서 테스트할 때는 짧은 값으로 돌아가도 넘어갈 수 있는데,
실제로는 라이브러리가 엄격하게 검증합니다.

배포 전 체크리스트에 "환경 변수 값 유효성 검증"을 포함시켜야 합니다.

### 4. Docker는 상태(State)를 직접 관리해주지 않는다

컨테이너는 immutable합니다. 재시작하면 컨테이너 내부에 저장된 파일은 사라집니다.
업로드 파일, DB 데이터, 로그 등 지속되어야 하는 것은 반드시 볼륨으로 관리해야 합니다.

---

## 현재 아키텍처 최종 상태

```
Android App
  └── Retrofit2 + OkHttp
  └── AuthInterceptor (JWT 자동 첨부)
  └── STOMP WebSocket (채팅, ?token=JWT)
        ↓ HTTP / WebSocket
OCI VM (138.2.117.37)
  └── Docker
        ├── cns-app:latest (Spring Boot 3.4.1)
        │     ├── REST API (8080)
        │     ├── WebSocket STOMP
        │     └── JWT 인증 (STATELESS)
        └── MySQL 8.0
              └── mysql_data (영속 볼륨)
```

CI/CD: GitHub Actions → GHCR → SSH deploy

---

## 남은 작업 (선택적)

| 우선순위 | 작업 | 용도 |
|----------|------|------|
| 높음 | GCP OCR API 키 발급 + `gcp-key.json` VM 배포 | 재료 사진 인식 기능 |
| 높음 | Firebase `serviceAccountKey.json` VM 배포 | FCM 푸시 알림 |
| 중간 | `.env`에 `GOOGLE_API_KEY` 추가 | Google Translate/Vision |
| 중간 | HTTPS 적용 (도메인 + Nginx + Let's Encrypt) | 실서비스 오픈 전 필수 |
| 낮음 | docker-compose v2 업그레이드 | ContainerConfig 버그 근본 해결 |

---

## 다음 시리즈 예고

- **HTTPS 적용**: Nginx 리버스 프록시 + Let's Encrypt 인증서 자동 갱신
- **FCM 푸시 알림**: Firebase Admin SDK + Spring Boot 연동
- **모니터링**: Actuator + Prometheus + Grafana로 서버 상태 대시보드 구성
