---
paths:
  - "docker-compose.yml"
  - "src/main/resources/**"
  - ".env.example"
  - "build.gradle"
---

# 실행 / Docker / API 수동 검증

- 평소 개발은 로컬 IDE / Gradle로 앱을 실행하고, `docker-compose.yml`의 인프라(MySQL, Redis, MinIO)만 띄우는 쪽을 권장한다.
- `docker-compose.yml`에는 앱 컨테이너(`backend`, `frontend`)도 정의되어 있어 `--build`로 전체를 한 번에 띄울 수도 있다(빠른 로컬 통합 확인, 프론트-백엔드 동시 데모 등). `backend`용 [`Dockerfile`](../../Dockerfile)은 `./gradlew bootJar -x test`로 빌드하고, `frontend`는 `../front-end/Dockerfile`(nginx)을 사용한다. 컨테이너 안에서는 `localhost` 대신 서비스명(`mysql`/`redis`/`minio`)으로 접속하도록 `docker-compose.yml`의 `backend.environment`가 `.env`의 `DB_URL`/`REDIS_HOST`/`MINIO_ENDPOINT` 값을 오버라이드한다.

```bash
docker compose up -d mysql redis minio   # 인프라만 기동 (앱은 IDE/Gradle로 실행)
docker compose up -d --build             # 인프라 + 백엔드 + 프론트 전체 기동
docker compose ps
docker compose logs -f backend
docker compose down       # 중지 (볼륨 보존)
docker compose down -v    # 중지 + 볼륨 삭제(초기화)
```

- 로컬 앱 실행은 compose MySQL + Flyway를 사용한다. 스키마의 주인은 Flyway 마이그레이션(`db/migration`)이고 JPA는 `ddl-auto: validate`로 검증만 한다. 스키마 변경은 기존 파일 수정이 아니라 새 `V{n}__*.sql` 추가로 한다.
- 테스트는 H2 in-memory(MySQL 모드, `create-drop`)로 Docker 없이 돈다. 테스트 설정은 `src/test/resources/application.yml`이 main 설정을 가리는(shadowing) 구조이므로 main 설정 파일명을 바꾸면 테스트 쪽도 같이 맞춘다.
- Compose/인프라 변경 시 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. Docker가 없으면 PR 검증 결과에 명시한다.

## 단일 서버 도메인 배포 (CORS)

- 프론트(`frontend`, nginx)가 `/api`를 `frontend/nginx.conf`에서 `backend` 컨테이너로 리버스 프록시한다. 브라우저 입장에서는 프론트와 API가 같은 도메인(origin)이라 일반적인 CORS preflight 문제가 없다.
- 그래도 WebSocket(`/api/ws`) Origin 검증(`WebSocketConfig`)과 `/api/v1/auth/token/refresh`의 Origin 가드(`AuthController`)는 요청의 `Origin` 헤더를 `ALLOWED_ORIGINS`와 직접 비교한다. 브라우저는 same-origin이어도 POST 등 요청에 `Origin` 헤더를 붙이므로, **배포 도메인을 `ALLOWED_ORIGINS`에 프로토콜까지 정확히 추가하지 않으면 403이 난다.** (`.env.example` 참고, 예: `https://youthpick.samchon.cloud`)
- 프론트 이미지는 `front-end/Dockerfile`에서 `VITE_API_BASE_URL=/api`로 빌드타임 고정한다(도메인 무관하게 항상 상대경로) — 로컬 `front-end/.env`의 절대경로(`http://localhost:8080/api`)는 vite dev 전용이며 Docker 빌드에는 반영되지 않는다.
- OAuth를 쓰면 `OAUTH_FRONTEND_CALLBACK_URI`도 배포 도메인의 콜백 경로로 바꾸고, 각 provider 콘솔의 redirect_uri도 함께 갱신한다.

## API 수동 검증

- 한글 query parameter는 raw URL에 넣지 말고 `--data-urlencode`를 사용한다:

```bash
curl -G http://localhost:8080/api/v1/policies \
  --data-urlencode 'keyword=월세' \
  --data-urlencode 'region=서울'
```

- actuator health(`/actuator/health`)와 API health를 구분한다. Redis나 DB가 꺼져 있으면 actuator health가 DOWN일 수 있다.
