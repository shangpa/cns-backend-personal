# OCI 배포 + Android 연동 시리즈

Spring Boot 백엔드를 OCI(Oracle Cloud Infrastructure)에 Docker로 배포하고
Android 앱과 실제 연동하기까지의 과정을 기록한 시리즈입니다.

## 챕터 목록

| 파일 | 제목 |
|------|------|
| [ch0-intro.md](ch0-intro.md) | 들어가며 — 리팩토링한 백엔드, 이제 실제로 배포해보자 |
| [ch1-oci-docker-setup.md](ch1-oci-docker-setup.md) | OCI VM 세팅 + Docker 배포 환경 구성 |
| [ch2-cicd-github-actions.md](ch2-cicd-github-actions.md) | CI/CD — GitHub Actions로 자동 배포 파이프라인 구축 |
| [ch3-android-api-migration.md](ch3-android-api-migration.md) | Android 앱 API 연동 — 리팩토링으로 바뀐 엔드포인트 수정 |
| [ch4-bugs-and-fixes.md](ch4-bugs-and-fixes.md) | 배포 후 발견한 버그 3가지와 해결 과정 |
| [ch5-retrospective.md](ch5-retrospective.md) | 마치며 — 배운 점과 남은 작업 |
| [troubleshooting.md](troubleshooting.md) | 트러블슈팅 모음 (에러별 상세 해결 방법) |

## 기술 스택

- **Backend**: Spring Boot 3.4.1, Spring Security 6, JWT (STATELESS)
- **Database**: MySQL 8.0
- **Infra**: OCI VM.Standard.E2.1.Micro (Always Free), Docker, docker-compose
- **CI/CD**: GitHub Actions → GHCR → SSH deploy
- **Mobile**: Android, Retrofit2, OkHttp, STOMP WebSocket
