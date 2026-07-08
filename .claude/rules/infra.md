---
paths:
  - "docker-compose.yml"
  - "src/main/resources/**"
  - ".env.example"
  - "build.gradle"
---

# 실행 / Docker / API 수동 검증

- **앱(백엔드)은 컨테이너화하지 않는다.** 로컬 IDE / Gradle로 실행하고, `docker-compose.yml`은 로컬 인프라(Redis, 필요 시 MySQL)만 띄운다.

```bash
docker compose up -d      # Redis (필요 시 MySQL) 기동
docker compose ps
docker compose down       # 중지 (볼륨 보존)
docker compose down -v    # 중지 + 볼륨 삭제(초기화)
```

- 로컬 기본 DB는 H2 in-memory이므로 MySQL 없이도 앱이 뜬다. MySQL로 붙일 때만 compose의 MySQL 서비스와 관련 환경변수를 활성화한다.
- Compose/인프라 변경 시 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. Docker가 없으면 PR 검증 결과에 명시한다.

## API 수동 검증

- 한글 query parameter는 raw URL에 넣지 말고 `--data-urlencode`를 사용한다:

```bash
curl -G http://localhost:8080/api/v1/policies \
  --data-urlencode 'keyword=월세' \
  --data-urlencode 'region=서울'
```

- actuator health(`/actuator/health`)와 API health를 구분한다. Redis나 DB가 꺼져 있으면 actuator health가 DOWN일 수 있다.
