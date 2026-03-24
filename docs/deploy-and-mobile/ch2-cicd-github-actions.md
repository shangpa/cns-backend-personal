# 챕터 2. CI/CD — GitHub Actions로 자동 배포 파이프라인 구축

> main 브랜치에 push하면 자동으로 빌드 → Docker 이미지 생성 → OCI VM 배포까지 이어지는 파이프라인입니다.

---

## 전체 흐름

```
git push origin main
        ↓
GitHub Actions 트리거
        ↓
1. ./gradlew build (테스트 포함)
2. Docker 이미지 빌드
3. GHCR(GitHub Container Registry)에 push
        ↓
OCI VM에 SSH 접속
        ↓
docker-compose pull && docker-compose up -d
```

---

## workflow 파일

`.github/workflows/deploy.yml`

```yaml
name: Deploy to OCI

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build -x test

      - name: Log in to GHCR
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/cns-backend:latest

      - name: Deploy to OCI VM
        uses: appleboy/ssh-action@v0.1.10
        with:
          host: ${{ secrets.OCI_HOST }}
          username: ubuntu
          key: ${{ secrets.OCI_SSH_KEY }}
          script: |
            cd ~/cns-backend
            docker-compose pull
            docker-compose up -d
            docker image prune -f
```

---

## GitHub Secrets 설정

Repository > Settings > Secrets and variables > Actions에서 추가:

| Secret 이름 | 값 |
|-------------|-----|
| `OCI_HOST` | VM 공인 IP (예: `138.2.117.37`) |
| `OCI_SSH_KEY` | `.pem` 파일 내용 전체 (`-----BEGIN RSA PRIVATE KEY-----` 포함) |

`GITHUB_TOKEN`은 GitHub이 자동으로 제공하므로 별도 설정 불필요합니다.

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## GHCR 이미지 권한 설정

기본적으로 GHCR에 push된 이미지는 private입니다.
VM에서 pull하려면 인증이 필요합니다.

**방법 1 (권장):** Package를 public으로 변경
- GitHub > Packages > cns-backend > Package settings > Change visibility > Public

**방법 2:** VM에서 GHCR 로그인
```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

---

## 배포 확인

```bash
# Actions 탭에서 워크플로우 상태 확인
# 또는 VM에서 직접 확인
docker ps
curl http://localhost:8080/actuator/health
```
