# 트러블슈팅 모음 — OCI 배포 + Android 연동

> 배포와 앱 연동 과정에서 마주친 에러들과 해결 방법을 정리했습니다.
> 구글링해도 안 나오는 것들 위주로 기록합니다.

---

## 목차

1. [403 Forbidden — 회원가입 차단](#1-403-forbidden--회원가입-차단)
2. [500 Error — JWT 키 길이 부족 (UnsupportedKeyException)](#2-500-error--jwt-키-길이-부족-unsupportedkeyexception)
3. [ContainerConfig KeyError — docker-compose + Docker 버전 충돌](#3-containerconfig-keyerror--docker-compose--docker-버전-충돌)
4. [업로드 파일 소실 — Docker 볼륨 미마운트](#4-업로드-파일-소실--docker-볼륨-미마운트)
5. [Swagger에 /login이 안 보임](#5-swagger에-login이-안-보임)
6. [curl 요청 무응답 (hang)](#6-curl-요청-무응답-hang)

---

## 1. 403 Forbidden — 회원가입 차단

### 에러 메시지

```json
{
  "timestamp": "2025-...",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/user/auth/join"
}
```

### 원인

Spring Security `SecurityConfig`에서 `/api/user/**` 경로 전체를 `.authenticated()`로 설정했는데,
회원가입 경로 `/api/user/auth/join`도 여기에 포함되어 차단되었습니다.

```java
// 문제가 된 설정
.requestMatchers("/api/user/**").authenticated()  // 너무 넓은 범위
```

Spring Security는 규칙을 **위에서 아래 순서로 매칭**합니다.
`permitAll`이 아래에 있어도 위의 `authenticated` 규칙이 먼저 적용됩니다.

### 해결

```java
// 수정 — permitAll 목록에 회원가입 경로 추가
.requestMatchers(
    "/login", "/", "/join",
    "/api/auth/**",
    "/api/user/auth/**"   // ← 추가
).permitAll()
.anyRequest().authenticated()
```

### 핵심 원칙

- `permitAll`할 경로는 `authenticated` 규칙보다 **앞에** 두거나
- `requestMatchers`를 구체적인 경로부터 먼저 정의해야 합니다.

---

## 2. 500 Error — JWT 키 길이 부족 (UnsupportedKeyException)

### 에러 메시지

```
docker-compose logs app | grep ERROR

io.jsonwebtoken.security.UnsupportedKeyException:
The specified key byte array is 120 bits which is not secure enough
for any JWT HMAC-SHA algorithm.
The JWT specification requires that HMAC-SHA keys MUST have a size >= 256 bits
```

### 원인

VM `.env`의 `JWT_SECRET` 값이 너무 짧았습니다.

```
JWT_SECRET=mysecret   # 9자 = 72bit → 256bit 미만 → 예외 발생
```

`io.jsonwebtoken` 라이브러리는 HMAC-SHA256 서명에 **최소 32자(256bit)** 키를 요구합니다.
짧은 키를 사용하면 서버가 기동 후 로그인 시도 시 500 에러를 던집니다.

### 해결

```bash
# VM SSH 접속 후
nano .env
```

```
# .env 수정
JWT_SECRET=이렇게32자이상의충분히긴문자열로바꿔주세요!!여기수정
```

안전한 키 생성 방법:
```bash
openssl rand -base64 48
# 출력된 64자 문자열을 JWT_SECRET 값으로 사용
```

수정 후 재시작:
```bash
docker-compose up -d
```

### 주의

`.env`는 절대 git에 커밋하지 마세요.
`.gitignore`에 `.env`가 포함되어 있는지 확인하세요.

```
# .gitignore
.env
.env.*
```

---

## 3. ContainerConfig KeyError — docker-compose + Docker 버전 충돌

### 에러 메시지

```
ERROR: for cns-app  'ContainerConfig'
Traceback (most recent call last):
  File "/usr/lib/python3/dist-packages/compose/cli/main.py", line ...
KeyError: 'ContainerConfig'
```

### 원인

**docker-compose v1.29.2 + Docker 28.x 버전 호환성 버그**입니다.

Docker Engine 28.x에서 컨테이너 검사(`docker inspect`) 결과의 JSON 구조가 바뀌었는데,
구버전 docker-compose(v1.29.2, Python 기반)가 기존 컨테이너 메타데이터를 읽을 때
`ContainerConfig` 키를 찾지 못해 발생합니다.

기존에 만들어진 컨테이너가 남아있을 때 `docker-compose up`을 실행하면 재현됩니다.

### 해결 (임시)

```bash
# 문제 컨테이너 확인
docker ps -a

# 강제 삭제 (컨테이너만 삭제, 볼륨은 유지됨)
docker rm -f <컨테이너_ID>

# 재생성
docker-compose up -d
```

### 해결 (근본)

docker-compose v2(Go 기반)로 업그레이드하면 이 버그가 없습니다.

```bash
# 구버전 제거
sudo apt-get remove docker-compose

# v2 설치
sudo curl -L \
  "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 확인
docker-compose --version   # Docker Compose version v2.x.x
```

---

## 4. 업로드 파일 소실 — Docker 볼륨 미마운트

### 증상

서버 재배포(컨테이너 재시작) 후 사용자가 올린 이미지/동영상이 모두 사라졌습니다.

### 원인

Spring Boot에서 업로드 파일을 컨테이너 내부 경로에 저장하고 있었습니다:

```yaml
# 잘못된 설정 (볼륨 없음)
app:
  image: cns-app:latest
  # volumes 없음 → 컨테이너 내부에만 저장
```

Docker 컨테이너는 기본적으로 레이어 기반 파일시스템을 사용합니다.
컨테이너를 삭제하거나 새 이미지로 재생성하면 컨테이너 레이어의 파일이 모두 초기화됩니다.

### 해결

```yaml
# docker-compose.yml 수정
app:
  volumes:
    - ./uploads:/app/uploads   # 호스트 디렉토리에 영속 저장
```

배포 전에 미리 디렉토리를 만들어두는 것이 좋습니다:

```bash
mkdir -p ~/cns-backend/uploads
```

### 원칙

상태(state)를 가지는 모든 것은 볼륨으로 빼야 합니다:

| 데이터 | 볼륨 설정 |
|--------|-----------|
| DB 데이터 | named volume (`mysql_data:/var/lib/mysql`) |
| 업로드 파일 | bind mount (`./uploads:/app/uploads`) |
| 설정 파일 | bind mount (`./config:/app/config`) |

---

## 5. Swagger에 /login이 안 보임

### 증상

`http://<서버IP>:8080/swagger-ui/index.html`에서 `/login` 엔드포인트가 목록에 없었습니다.

### 원인

`/login`은 Spring Security가 처리하는 경로로, Spring MVC 컨트롤러에 매핑되지 않습니다.
Swagger는 `@RestController`나 `@Controller`가 붙은 클래스의 엔드포인트만 문서화합니다.

### 테스트 방법

Swagger에 없어도 실제로는 동작합니다. curl로 직접 테스트하면 됩니다:

```bash
curl -X POST http://<서버IP>:8080/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"testuser","password":"test1234"}'
```

성공 시 응답 헤더에 `Authorization: Bearer <JWT토큰>`이 담겨 옵니다.

---

## 6. curl 요청 무응답 (hang)

### 증상

curl 명령을 실행하면 응답이 없고 계속 대기 상태가 됩니다.

```bash
curl -X POST http://138.2.117.37:8080/login -d '...'
# (응답 없음, Ctrl+C로 종료해야 함)
```

### 원인 체크리스트

**1. 포트가 열려있는지 확인**

```bash
# 다른 터미널에서
nc -zv 138.2.117.37 8080
```

안 되면 OCI Security List와 OS iptables 확인:
```bash
sudo iptables -L INPUT | grep 8080
```

**2. 컨테이너가 실행 중인지 확인**

```bash
docker ps | grep cns-app
docker-compose logs --tail=50 app
```

**3. 애플리케이션이 기동 완료됐는지 확인**

Spring Boot는 DB 연결 등 초기화가 끝나야 요청을 받습니다:

```bash
docker-compose logs app | grep "Started CnsApplication"
```

이 로그가 나오기 전에 요청을 보내면 hang이 걸릴 수 있습니다.
