# 챕터 4. 배포 후 발견한 버그 3가지와 해결 과정

> "코드가 로컬에서 잘 되는데 서버에선 왜 안 되지?" 배포하면 항상 새로운 문제가 나옵니다.
> 이번에 마주친 버그 3가지와 해결 과정을 기록합니다.

---

## 버그 1. 회원가입 403 Forbidden

### 증상

Android에서 회원가입 요청을 보냈더니 403이 돌아왔습니다.

```json
{
  "timestamp": "2025-...",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/user/auth/join"
}
```

### 원인 분석

`SecurityConfig.java`를 보니 `permitAll` 목록에 회원가입 경로가 없었습니다.

```java
// 기존 설정
.requestMatchers(
    "/login", "/", "/join", "/admin/join",
    "/api/auth/**"          // 구버전 경로만 허용
).permitAll()
.requestMatchers("/api/user/**").authenticated()  // 이 규칙이 위보다 먼저 적용됨
```

리팩토링 과정에서 회원가입 경로가 `/api/user/auth/join`으로 바뀌었는데,
`/api/user/**`를 전부 인증 필요로 막아놓은 규칙이 더 구체적인 permitAll보다 먼저 매칭되었습니다.

Spring Security는 **위에서 아래로 순서대로** 매칭합니다.
`/api/user/**` authenticated 규칙이 `/api/user/auth/**` permitAll보다 앞에 있으면
회원가입도 막힙니다.

### 해결

```java
// 수정 후
.requestMatchers(
    "/login", "/", "/join", "/admin/join",
    "/api/auth/**",
    "/api/user/auth/**"     // 회원가입 경로 추가
).permitAll()
```

### 교훈

리팩토링으로 URL 구조가 바뀌면 SecurityConfig의 permitAll 목록도 같이 검토해야 합니다.
특히 인증이 필요 없는 경로(회원가입, 로그인, 공개 조회)가 새 URL에서도 열려있는지 확인하세요.

---

## 버그 2. 로그인 500 에러 — JWT 키 길이 부족

### 증상

회원가입은 됐는데 로그인 시 500 에러가 발생했습니다.

```bash
curl -X POST http://138.2.117.37:8080/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"test","password":"1234"}'
# 응답 없음 → 잠시 후 500
```

### 원인 분석

Docker 컨테이너 로그를 확인했습니다:

```bash
docker-compose logs app | grep ERROR
```

```
io.jsonwebtoken.security.UnsupportedKeyException:
The specified key byte array is 120 bits which is not secure enough for any JWT HMAC-SHA algorithm.
The JWT specification requires that HMAC-SHA keys MUST have a size >= 256 bits
```

VM의 `.env` 파일에서 `JWT_SECRET` 값이 너무 짧았습니다:

```
JWT_SECRET=mysecret   # 9자 = 72bit → 불충분
```

### 해결

VM에 SSH 접속 후 `.env` 파일 수정:

```bash
nano .env
# JWT_SECRET=최소32자이상의충분히긴랜덤문자열로교체합니다!
```

이후 컨테이너 재시작:

```bash
docker-compose up -d
```

### 교훈

JWT HMAC-SHA256은 **최소 32자(256bit)** 키가 필요합니다.
로컬에서는 짧은 키로도 돌아가는 것처럼 보일 수 있지만,
io.jsonwebtoken 라이브러리가 엄격하게 검증합니다.

프로덕션 환경에서는 다음 방법으로 안전한 키를 생성하세요:

```bash
# 랜덤 64자 문자열 생성
openssl rand -base64 48
```

---

## 버그 3. docker-compose up 실패 — ContainerConfig KeyError

### 증상

`.env` 수정 후 `docker-compose up -d`를 실행했더니 에러가 발생했습니다:

```
ERROR: for cns-app  'ContainerConfig'
Traceback (most recent call last):
  ...
KeyError: 'ContainerConfig'
```

### 원인 분석

**docker-compose v1.29.2 + Docker 28.x 버전 간 호환성 버그**입니다.

Docker 28.x에서 컨테이너 메타데이터 포맷이 바뀌었는데,
구버전 docker-compose(v1.29.2)가 이를 파싱하지 못합니다.
기존에 생성된 컨테이너의 메타데이터를 읽으려 할 때 발생합니다.

### 해결

문제가 된 컨테이너를 강제 삭제하고 새로 생성합니다:

```bash
# 문제 컨테이너 ID 확인
docker ps -a

# 강제 삭제
docker rm -f <컨테이너_ID>

# 재생성
docker-compose up -d
```

### 근본적 해결책

docker-compose v2로 업그레이드하면 이 버그가 없습니다:

```bash
sudo apt-get remove docker-compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

---

## 버그 4. 파일 업로드 후 서버 재시작 시 이미지 소실 (사전 예방)

### 증상 (발생 전 발견)

`docker-compose.yml`을 검토하다가 `uploads` 폴더에 볼륨 마운트가 없음을 발견했습니다.

Spring Boot에서 업로드한 이미지/동영상은 컨테이너 내부 `/app/uploads`에 저장되는데,
컨테이너가 재시작되면 컨테이너 레이어가 초기화되므로 파일이 모두 사라집니다.

### 해결 (사전 적용)

```yaml
app:
  volumes:
    - ./uploads:/app/uploads    # 호스트 디렉토리에 영속 저장
```

이렇게 하면 컨테이너가 재시작되어도 호스트의 `./uploads` 폴더에 파일이 남아있습니다.

### 교훈

상태(state)를 가지는 모든 것은 볼륨으로 빼야 합니다:
- 데이터베이스 데이터: `mysql_data` 볼륨
- 업로드 파일: `./uploads` 바인드 마운트
- 설정 파일: 바인드 마운트 또는 시크릿
