# 챕터 0. 들어가며 — 리팩토링한 백엔드, 이제 실제로 배포해보자

> 이 시리즈는 Spring Boot 백엔드를 OCI(Oracle Cloud Infrastructure) VM에 Docker로 배포하고,
> Android 앱과 실제로 연동하기까지의 과정을 기록한 글입니다.

---

## 이 시리즈에서 다루는 것

리팩토링 시리즈에서 코드를 정리했다면, 이번엔 그 코드를 **실제 서버에 올리고 앱과 연결**하는 과정입니다.

진행 순서는 다음과 같습니다.

| 챕터 | 주제 |
|------|------|
| 1 | OCI VM 세팅 + Docker 배포 환경 구성 |
| 2 | CI/CD — GitHub Actions로 자동 배포 파이프라인 구축 |
| 3 | Android 앱 API 연동 — 리팩토링으로 바뀐 엔드포인트 수정 |
| 4 | 배포 후 발견한 버그 3가지와 해결 과정 |
| 5 | 마치며 — 남은 작업과 배운 점 |

---

## 왜 OCI를 선택했나

클라우드 무료 티어를 비교했을 때 OCI의 Always Free 인스턴스가 가장 넉넉했습니다.

- **AWS EC2 t2.micro**: 750시간/월 (12개월만 무료)
- **GCP e2-micro**: 월 1개 무료 (사양 낮음)
- **OCI VM.Standard.E2.1.Micro**: 2개까지 **영구 무료**, 메모리 1GB

개인 프로젝트 서버를 오래 운영할 계획이라 OCI를 선택했습니다.

---

## 전체 스택 구성

```
Android App (Retrofit2 + OkHttp)
        ↓ HTTP/REST
OCI VM (Ubuntu 22.04)
  └── Docker
        ├── cns-app (Spring Boot 3.4.1)
        └── MySQL 8.0
```

JWT 인증, WebSocket STOMP 채팅, 파일 업로드(이미지/동영상) 기능이 포함된 SNS형 레시피 공유 앱입니다.
