---
paths:
  - "docker-compose.yml"
  - "src/main/resources/**"
  - ".env.example"
  - "build.gradle"
---

# 실행 / Docker / API 수동 검증

- **앱(백엔드)은 컨테이너화하지 않는다.** 로컬 IDE / Gradle로 실행하고, `docker-compose.yml`은 로컬 인프라(MySQL, Redis)만 띄운다.

```bash
docker compose up -d      # MySQL + Redis 기동 (앱 실행 전 필수)
docker compose ps
docker compose down       # 중지 (볼륨 보존)
docker compose down -v    # 중지 + 볼륨 삭제(초기화)
```

- 로컬 앱 실행은 compose MySQL + Flyway를 사용한다. 스키마의 주인은 Flyway 마이그레이션(`db/migration`)이고 JPA는 `ddl-auto: validate`로 검증만 한다. 스키마 변경은 기존 파일 수정이 아니라 새 `V{n}__*.sql` 추가로 한다.
- 테스트는 H2 in-memory(MySQL 모드, `create-drop`)로 Docker 없이 돈다. 테스트 설정은 `src/test/resources/application.yml`이 main 설정을 가리는(shadowing) 구조이므로 main 설정 파일명을 바꾸면 테스트 쪽도 같이 맞춘다.
- Compose/인프라 변경 시 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. Docker가 없으면 PR 검증 결과에 명시한다.

## API 수동 검증

- 한글 query parameter는 raw URL에 넣지 말고 `--data-urlencode`를 사용한다:

```bash
curl -G http://localhost:8080/api/v1/policies \
  --data-urlencode 'keyword=월세' \
  --data-urlencode 'region=서울'
```

- actuator health(`/actuator/health`)와 API health를 구분한다. Redis나 DB가 꺼져 있으면 actuator health가 DOWN일 수 있다.
