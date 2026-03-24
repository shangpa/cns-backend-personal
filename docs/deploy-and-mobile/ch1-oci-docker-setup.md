# 챕터 1. OCI VM 세팅 + Docker 배포 환경 구성

> OCI 무료 인스턴스에 Ubuntu를 올리고, Docker + docker-compose로 Spring Boot + MySQL을 띄우는 과정입니다.

---

## 1. OCI 인스턴스 생성

Oracle Cloud 콘솔에서 Compute > Instances > Create Instance로 진행합니다.

**선택 사항:**
- Image: Canonical Ubuntu 22.04
- Shape: VM.Standard.E2.1.Micro (Always Free)
- 네트워크: 공인 IP 자동 할당

인스턴스 생성 시 SSH 키 페어를 발급받아 `.pem` 파일을 저장해둡니다.

---

## 2. 방화벽(Security List) 설정

OCI는 기본적으로 22번 포트(SSH)만 열려 있습니다.
애플리케이션 포트를 추가로 열어야 합니다.

Networking > Virtual Cloud Networks > Security Lists에서 Ingress Rules 추가:

| 포트 | 용도 |
|------|------|
| 22 | SSH (기본 제공) |
| 8080 | Spring Boot |
| 3306 | MySQL (필요 시) |

OS 레벨 방화벽도 열어줍니다:

```bash
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo netfilter-persistent save
```

---

## 3. Docker + docker-compose 설치

```bash
# Docker 설치
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# docker-compose 설치
sudo apt-get install docker-compose -y

# 버전 확인
docker --version        # Docker version 28.x.x
docker-compose --version  # docker-compose version 1.29.2
```

---

## 4. 프로젝트 구성 파일

### docker-compose.yml

```yaml
version: "3"

services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - cns-network

  app:
    image: ghcr.io/${GITHUB_USERNAME}/${IMAGE_NAME}:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/${DB_NAME}
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    volumes:
      - ./uploads:/app/uploads    # 이미지/동영상 파일 영속성
    depends_on:
      - db
    networks:
      - cns-network

volumes:
  mysql_data:

networks:
  cns-network:
```

### .env 파일 (VM에만 존재, git에 올리지 않음)

```
DB_ROOT_PASSWORD=강한비밀번호
DB_NAME=cns_db
DB_USERNAME=cns_user
DB_PASSWORD=강한비밀번호
JWT_SECRET=최소32자이상의랜덤문자열을여기에넣으세요!!
GITHUB_USERNAME=your-github-username
IMAGE_NAME=cns-backend
```

> **주의**: JWT_SECRET은 반드시 32자 이상이어야 합니다.
> HMAC-SHA256 알고리즘이 최소 256bit(32바이트) 키를 요구합니다.
> 짧으면 서버 기동 시 `UnsupportedKeyException`이 발생합니다.

---

## 5. 애플리케이션 실행

```bash
docker-compose pull
docker-compose up -d

# 로그 확인
docker-compose logs -f app
```

정상 기동 시 로그 마지막에 다음이 출력됩니다:
```
Started CnsApplication in 12.345 seconds
```

---

## 트러블슈팅: ContainerConfig KeyError

docker-compose v1.29.2 + Docker 28.x 조합에서 간헐적으로 다음 에러가 발생합니다:

```
KeyError: 'ContainerConfig'
```

이는 docker-compose 구버전이 Docker 신버전의 컨테이너 메타데이터 포맷 변경을 따라가지 못하는 버그입니다.

**해결 방법:**
```bash
# 문제 컨테이너 강제 삭제 후 재생성
docker rm -f <컨테이너_ID>
docker-compose up -d
```
