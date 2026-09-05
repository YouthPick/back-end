# Elasticsearch + nori + Spring 연결 — 직접 해보기

> 목표: 빈 상태에서 시작해 **ES 컨테이너 기동 → nori 설치 확인 → Spring 앱과 연결**까지.
> 각 단계에 **확인 방법**이 있다. 확인이 통과해야 다음으로 넘어간다.

## 전체 순서

```
1. ES 컨테이너 띄우기 (nori 없이)      — 먼저 맨몸으로 뜨는지 본다
2. nori 설치                          — Dockerfile 로 굽는다
3. nori 동작 확인                      — 한국어가 실제로 쪼개지는지
4. 사전 파일 자리 만들기                — 나중에 채울 빈 파일
5. Spring 의존성 추가
6. Spring 설정 (yml)
7. 연결 확인                          — 앱이 ES를 잡는지
```

---

# 1. ES 컨테이너 띄우기 (nori 없이)

먼저 **플러그인 없이** 순정 ES가 뜨는지 확인한다. 문제가 생기면 원인이 ES인지 nori인지 가려야 하기 때문.

## 1-1. `docker-compose.yml` 에 서비스 추가

기존 `services:` 아래, `backend:` 앞에 넣는다.

```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.18.8
    container_name: youthpick-es
    environment:
      - discovery.type=single-node       # 단일 노드 (클러스터 구성 안 함)
      - xpack.security.enabled=false     # 로컬이라 인증 끔
      - ES_JAVA_OPTS=-Xms512m -Xmx512m   # JVM 힙. 안 주면 시스템 메모리의 절반을 먹는다
    ports:
      - "${ES_PORT:-9200}:9200"
    volumes:
      - youthpick-es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:9200/_cluster/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 40s
```

파일 맨 아래 `volumes:` 블록에 한 줄 추가한다.

```yaml
volumes:
  youthpick-mysql-data:
  youthpick-redis-data:
  youthpick-minio-data:
  youthpick-es-data:      # ← 추가
```

> **`backend` 의 `depends_on` 에는 넣지 않는다.** ES가 죽어도 앱은 떠야 하고,
> 검색은 기존 LIKE 경로로 폴백해야 하기 때문(뒤 단계에서 구현).

## 1-2. 띄우기

```bash
cd D:\develop\project\ClaudeCode\test\Sesac\team-first-project\youth-pick
docker compose up -d elasticsearch
```

## ✅ 확인 1

```bash
docker compose ps elasticsearch
```
`STATUS` 가 `Up ... (healthy)` 가 될 때까지 기다린다 (40초~1분).

```bash
curl http://localhost:9200
```

이렇게 나오면 성공:
```json
{
  "name" : "...",
  "cluster_name" : "docker-cluster",
  "version" : { "number" : "8.18.8", ... },
  "tagline" : "You Know, for Search"
}
```

<details>
<summary>안 되면</summary>

- `docker compose logs elasticsearch` 로 로그 확인
- `max virtual memory areas vm.max_map_count [65530] is too low` → Docker Desktop 재시작
- 메모리 부족 → Docker Desktop 설정에서 메모리를 4GB 이상으로
</details>

---

# 2. nori 설치

## 2-1. 왜 Dockerfile 인가

`docker exec` 로 컨테이너에 들어가서 설치해도 동작하지만, **컨테이너를 다시 만들면 사라진다.**
플러그인은 `/usr/share/elasticsearch/plugins/` 에 설치되는데 여기는 볼륨이 아니다.
데이터 볼륨은 남으므로 "데이터는 있는데 분석기가 없는" 상태가 되어 원인 찾기가 어려워진다.

이미지에 구워두면 몇 번을 다시 만들어도 항상 있고, 팀원이 clone 해서 `docker compose up` 해도 똑같이 동작한다.

## 2-2. `es/Dockerfile` 생성

프로젝트 루트에 `es/` 폴더를 만들고 그 안에 `Dockerfile`:

```dockerfile
# 버전은 Spring Boot 가 관리하는 클라이언트 버전과 맞춘다.
# Spring Boot 3.5.16 → spring-data-elasticsearch 5.5.13 → elasticsearch-java 8.18.8
FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.8

RUN bin/elasticsearch-plugin install --batch analysis-nori
```

> `--batch` 는 설치 중 물어보는 확인 프롬프트를 자동 승인한다. 없으면 빌드가 멈춘다.
>
> **버전을 클라이언트와 맞추는 이유:** Java 클라이언트와 ES 서버의 minor 버전이 어긋나면
> 미묘한 호환성 문제가 난다. 클라이언트 버전은 이렇게 확인한다:
> ```bash
> ./gradlew dependencies --configuration compileClasspath | grep elasticsearch-java
> ```
> (5단계에서 의존성을 추가한 뒤에 확인 가능)

## 2-3. compose 를 image → build 로 변경

```yaml
  elasticsearch:
    build: ./es          # ← image: ... 를 이걸로 교체
    container_name: youthpick-es
    ...
```

## 2-4. 다시 빌드해서 띄우기

```bash
docker compose up -d --build elasticsearch
```

`--build` 를 빼면 기존 이미지를 그대로 쓴다. 처음 빌드는 몇 분 걸린다.

## ✅ 확인 2

```bash
docker exec youthpick-es bin/elasticsearch-plugin list
```

```
analysis-nori
```

한 줄 나오면 성공.

---

# 3. nori 동작 확인

설치됐다고 끝이 아니다. **실제로 한국어를 쪼개는지** 봐야 한다.

## 3-1. 문제: Windows 셸이 한글을 깨뜨린다

Git Bash / PowerShell 에서 curl 로 한글을 인라인으로 보내면 CP949 로 인코딩돼 ES 가 400 을 뱉는다.

```bash
curl -XGET "localhost:9200/_analyze" -H 'Content-Type: application/json' \
  -d '{"tokenizer":"nori_tokenizer","text":"청년 일자리"}'
```
```json
{"error":{"reason":"Invalid UTF-8 start byte 0xb3", ...}, "status":400}
```

## 3-2. 해결: UTF-8 로 보내는 헬퍼

`es/analyze.py` 를 만든다.

```python
# -*- coding: utf-8 -*-
"""ES _analyze 호출 헬퍼. Windows 셸의 한글 인코딩 문제를 우회한다."""
import json
import sys
import urllib.request

ES = "http://localhost:9200"


def analyze(text, tokenizer="nori_tokenizer"):
    body = {"tokenizer": tokenizer, "text": text}
    req = urllib.request.Request(
        f"{ES}/_analyze",
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req) as res:
        return json.loads(res.read().decode("utf-8"))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    text = " ".join(sys.argv[1:])
    tokens = [t["token"] for t in analyze(text)["tokens"]]
    print(f"{text}\n    -> {' · '.join(tokens)}")
```

## ✅ 확인 3

```bash
python es/analyze.py "청년 일자리를 지원하는 정책"
```

```
청년 일자리를 지원하는 정책
    -> 청년 · 일 · 자리 · 를 · 지원 · 하 · 는 · 정책
```

**조사 `를`, `하`, `는` 이 분리되면 nori 가 동작하는 것이다.**

> `일자리` 가 `일 · 자리` 로 쪼개진 것에 주목. `decompound_mode` 기본값이 `discard` 라
> 복합어를 쪼갠 뒤 원본을 버리기 때문이다. 다음 단계(인덱스 매핑)에서 `mixed` 로 바꾼다.
> 지금은 "nori 가 동작한다"만 확인하면 된다.

---

# 4. 사전 파일 자리 만들기

인덱스 매핑에서 사용자 사전·동의어 사전을 참조할 예정이라 **파일이 없으면 ES 가 에러를 낸다.**
빈 파일을 미리 만들어 둔다.

## 4-1. 파일 2개

`es/config/userdict_ko.txt`
```
# nori 사용자 사전 — 쪼개지면 안 되는 복합어를 한 줄에 하나씩.
# 이 파일을 고치면 재색인이 필요하다(색인 시점에 적용되므로).
```

`es/config/synonym_ko.txt`
```
# 동의어 사전 (Solr 형식) — 검색 시점에만 적용된다.
# 등가형:  일자리, 취업, 채용, 구직
# 지정형:  알바, 아르바이트 => 아르바이트
```

## 4-2. compose 에 마운트 추가

```yaml
    volumes:
      - youthpick-es-data:/usr/share/elasticsearch/data
      - ./es/config:/usr/share/elasticsearch/config/analysis   # ← 추가
```

> ES 는 보안상 `config/` 하위 경로만 사전 파일로 읽는다. 다른 경로에 두면 거부된다.

```bash
docker compose up -d elasticsearch
```

## ✅ 확인 4

```bash
docker exec youthpick-es ls config/analysis
```
```
synonym_ko.txt
userdict_ko.txt
```

---

# 5. Spring 의존성 추가

## 5-1. `build.gradle`

`dependencies { }` 안에 한 줄:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
```

버전은 안 쓴다 — Spring Boot 플러그인이 관리한다.

> **`@Document` / `ElasticsearchRepository` 는 쓰지 않을 것이다.**
> 이 스타터를 쓰는 이유는 `ElasticsearchClient`(공식 Java 클라이언트) 자동구성을 얻기 위해서다.
> 우리 검색은 필터 8개 + 조건부 정렬 + 패싯 집계라 Repository 추상화로 표현되지 않는다.

## ✅ 확인 5

```bash
./gradlew compileJava
./gradlew dependencies --configuration compileClasspath | grep elasticsearch-java
```
```
co.elastic.clients:elasticsearch-java:8.18.8
```

**여기 나온 버전이 `es/Dockerfile` 의 ES 버전과 같아야 한다.** 다르면 Dockerfile 을 맞춰 수정하고
`docker compose up -d --build elasticsearch` 로 다시 빌드한다.

---

# 6. Spring 설정

이 프로젝트의 yml 규칙을 먼저 이해하고 넣어야 한다.

| 종류 | 어디에 | 기존 예시 |
|---|---|---|
| 환경마다 다른 **주소** | `application-local.yml` | `spring.data.redis.host` |
| 어디서든 같은 **동작 규칙** | `application.yml` | `spring.http.client.read-timeout` |
| **안전 기본값 off** | `application.yml` | `spring.flyway.enabled: false` |
| 그걸 **켜기** | `application-local.yml` | `spring.flyway.enabled: true` |

## 6-1. `application.yml` — 동작 규칙 + 안전 기본값

`spring:` 아래에 추가:
```yaml
  elasticsearch:
    # 접속 주소(uris)는 환경마다 달라 각 프로필이 정한다(redis와 같은 방식).
    # 여기엔 환경과 무관한 동작 규칙만 둔다 — ES가 느려질 때 기본값(30s)을 기다리면
    # 폴백이 있어도 사용자는 이미 떠난 뒤다. 짧게 끊고 LIKE 경로로 넘긴다.
    connection-timeout: ${ES_CONNECT_TIMEOUT:1s}
    socket-timeout: ${ES_SOCKET_TIMEOUT:3s}
```

`youthpick:` 아래에 추가:
```yaml
  search:
    # 기본 off(안전 기본값) — sync.scheduler와 같은 방식.
    # ES 없이도 앱이 뜨고 LIKE로 검색된다.
    enabled: ${SEARCH_ES_ENABLED:false}
    # 코드가 보는 이름. 실제 인덱스는 policy_v1, policy_v2… 이고 이 alias가 가리킨다.
    alias: ${SEARCH_ES_ALIAS:policy}
```

## 6-2. `application-local.yml` — 주소 + 켜기

`spring:` 아래:
```yaml
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
```

`youthpick:` 아래:
```yaml
  search:
    enabled: ${SEARCH_ES_ENABLED:true}   # 로컬은 compose로 ES를 띄우므로 켠다
```

## 6-3. 설정 바인딩 클래스

`youthpick.search.*` 를 코드에서 읽으려면 클래스가 필요하다.
이 프로젝트는 `@ConfigurationPropertiesScan` 이 켜져 있고(`YouthpickApplication`),
기존 `SiteProperties` 처럼 **record 하나**로 끝낸다.

`src/main/java/com/bop/youthpick/policy/search/PolicySearchProperties.java`

```java
package com.bop.youthpick.policy.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 정책 검색(Elasticsearch) 설정.
 *
 * <p>{@code enabled}가 false면 ES를 아예 호출하지 않고 기존 LIKE 검색만 쓴다 — ES가 없는
 * 환경(테스트·CI)에서도 앱이 그대로 뜨게 하기 위함이며, {@code youthpick.sync.scheduler.enabled}와
 * 같은 "안전 기본값 off" 방식이다.
 *
 * <p>{@code alias}는 코드가 보는 인덱스 이름이다. 실제 인덱스는 {@code policy_v1},
 * {@code policy_v2}… 이고 이 alias가 그중 하나를 가리킨다. 분석기 설정은 색인 시점에
 * 적용되므로 매핑을 바꾸면 재색인이 필수인데, alias가 있으면 새 인덱스를 채운 뒤 alias만
 * 옮겨 검색 중단 없이 전환할 수 있다.
 */
@ConfigurationProperties(prefix = "youthpick.search")
public record PolicySearchProperties(boolean enabled, String alias) {}
```

`@ConfigurationPropertiesScan` 덕분에 **등록 코드가 따로 필요 없다.** 만들면 바로 주입된다.

---

# 7. 연결 확인

앱이 실제로 ES 를 잡는지 본다.

## 7-1. 확인용 임시 컴포넌트

`src/main/java/com/bop/youthpick/policy/search/EsConnectionCheck.java`
(확인이 끝나면 지울 파일)

```java
package com.bop.youthpick.policy.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsConnectionCheck implements ApplicationRunner {

    private final ElasticsearchClient client;
    private final PolicySearchProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("[ES] 검색 비활성(youthpick.search.enabled=false) — 연결 확인 생략");
            return;
        }
        try {
            var info = client.info();
            log.info("[ES] 연결 성공 — version={} cluster={}",
                    info.version().number(), info.clusterName());
        } catch (Exception e) {
            // ES가 없어도 앱은 떠야 한다. 로그만 남기고 넘어간다.
            log.warn("[ES] 연결 실패 — 검색은 LIKE 경로로 동작한다: {}", e.getMessage());
        }
    }
}
```

## 7-2. 실행

```bash
docker compose up -d mysql redis minio elasticsearch
./gradlew bootRun
```

## ✅ 확인 7

로그에 이렇게 뜨면 성공:
```
[ES] 연결 성공 — version=8.18.8 cluster=docker-cluster
```

**일부러 실패도 시켜본다** (폴백이 동작할 조건을 확인):
```bash
docker compose stop elasticsearch
./gradlew bootRun
```
```
[ES] 연결 실패 — 검색은 LIKE 경로로 동작한다: ...
```
**여기서 앱이 죽지 않고 뜨는 게 중요하다.** 죽으면 `depends_on` 이나 설정을 잘못 넣은 것이다.

확인이 끝나면 `EsConnectionCheck.java` 는 지운다.

---

# 완료 후 상태

```
youth-pick/
├── es/
│   ├── Dockerfile                    ES 8.18.8 + analysis-nori
│   ├── analyze.py                    _analyze 헬퍼
│   └── config/
│       ├── userdict_ko.txt           (빈 파일)
│       └── synonym_ko.txt            (빈 파일)
├── docker-compose.yml                elasticsearch 서비스 + 볼륨
├── build.gradle                      spring-boot-starter-data-elasticsearch
└── src/main/
    ├── resources/
    │   ├── application.yml           타임아웃 + search.enabled=false + alias
    │   └── application-local.yml     uris + search.enabled=true
    └── java/com/bop/youthpick/policy/search/
        └── PolicySearchProperties.java
```

## 다음 단계

```
▶ 인덱스 매핑 JSON 작성      ← nori decompound_mode, 동의어, 필드 가중치가 들어가는 곳
  인덱스 생성 코드
  정책 → ES 문서 변환
  색인 코드 (bulk)
  검색 코드
  PolicyService 폴백
```
