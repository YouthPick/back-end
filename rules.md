# BOP Back-end Agent Rules

이 문서는 `back-end-proto`에서 작업하는 모든 에이전트와 개발자가 따라야 하는 백엔드 구현 규칙이다. 새 기능을 만들거나 기존 코드를 수정하기 전에 반드시 이 파일을 먼저 읽고, 이 규칙과 충돌하는 구현은 하지 않는다.

## 1. 작업 흐름

- 기획 변경이나 구현 계획이 필요한 작업은 먼저 `docs` 레포에 계획 문서를 올리고, 관련 GitHub Issue를 만든 뒤 작업한다.
- 백엔드 작업은 issue 번호가 포함된 브랜치에서 진행한다.
- 구현 후에는 테스트를 직접 실행하고, PR에 실제 검증 결과를 적는다.
- 단순 설명이나 스텁으로 끝내지 않는다. 동작하는 코드와 검증 결과를 남긴다.

## 2. 기술 기준

- Java 21을 사용한다.
- Spring Boot 3.5.16 기준으로 작성한다.
- Spring Web, Spring Data JPA, Spring Data Redis, Spring Security, Bean Validation을 프로젝트 기본 스택으로 본다.
- Gradle Wrapper(`./gradlew`)를 사용한다.
- 테스트 실행 시 로컬 JDK가 없으면 `/home/heeho3/.jdks/temurin-21`을 우선 사용한다.

```bash
export JAVA_HOME=/home/heeho3/.jdks/temurin-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test
```

## 3. 패키지 구조

패키지는 도메인 단위 폴더 구조로 만든다. 다만 도메인 내부는 과하게 DDD식으로 나누지 않고 `controller`, `service`, `repository`, `dto`, `entity` 중심의 단순 구조를 사용한다.

현재 기준 구조는 다음과 같다.

```text
com.bop.youthpick
├── global
│   ├── common
│   ├── config
│   └── error
├── admin
│   └── controller
├── policy
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
└── sync
    ├── controller
    ├── service
    ├── repository
    ├── dto
    └── entity
```

새 도메인을 추가할 때는 아래 형태를 따른다.

```text
com.bop.youthpick.{domain}
├── controller
├── service
├── repository
├── dto
└── entity
```

각 패키지 역할은 다음과 같다.

- `controller`: HTTP Controller와 API 입출력 조립을 담당한다.
- `service`: 비즈니스 흐름, scheduler, lock, 외부 API client, 기술 상세 구현을 둔다.
- `repository`: Spring Data JPA repository interface를 둔다.
- `dto`: 요청/응답 DTO, service 결과 DTO, 외부 API payload DTO를 둔다.
- `entity`: JPA Entity, domain enum을 둔다.
- `global`: 여러 도메인이 공유하는 공통 응답, 설정, 예외 처리만 둔다.

금지 구조는 다음과 같다.

```text
com.bop.youthpick.{domain}
├── api
├── application
├── domain
├── infrastructure
└── external
```

## 4. Controller 규칙

Controller는 얇게 유지한다. HTTP 요청 파싱, Bean Validation, Service 호출, 응답 DTO 변환만 담당한다. 비즈니스 판단, 중복 검사, 외부 API 호출, Entity 상태 변경 로직은 Controller에 두지 않는다.

```java
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicySearchService policySearchService;

    public PolicyController(PolicySearchService policySearchService) {
        this.policySearchService = policySearchService;
    }
}
```

- 생성자 주입을 사용한다.
- 필드 주입(`@Autowired` field injection)은 금지한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 요청 body를 받는 파라미터에는 `@Valid @RequestBody`를 붙인다.

## 5. DTO 규칙

요청 DTO와 응답 DTO는 각 도메인의 `dto` 패키지에 둔다. DTO는 API 계약을 표현하는 객체이며 Entity를 외부에 노출하지 않기 위한 경계다.

### 5.1 요청 DTO

- 요청 DTO는 `record`를 사용할 수 있다. 다만 안정적인 field-level validation 응답이 필요한 경우 필드에 Bean Validation annotation을 명확히 붙인다.
- enum 값을 직접 바인딩하면 Jackson deserialization 단계에서 터질 수 있다. 잘못된 enum 값을 `C001` 입력값 오류로 내려야 하면 문자열 필드 + `@Pattern`을 사용한다.
- 모든 사용자 입력 필드는 의도를 드러내는 validation annotation을 붙인다.
- validation message는 한국어로 작성한다.

```java
public record PolicySyncJobStartRequest(
        @Pattern(regexp = "FULL|DELTA", message = "mode는 FULL 또는 DELTA만 허용됩니다.")
        String mode
) {
    public PolicySyncJobMode normalizedMode() {
        if (mode == null || mode.isBlank()) {
            return PolicySyncJobMode.FULL;
        }
        return PolicySyncJobMode.valueOf(mode);
    }
}
```

요청 DTO에 validation이 없거나 Controller에서 `@Valid` 없이 받는 코드는 금지한다.

### 5.2 응답 DTO

- 응답 DTO는 Entity를 받아 정적 팩토리 메서드 `from(...)`으로 생성한다.
- 응답 DTO에서 Entity의 민감 필드, 내부 상태, raw payload, secret 값을 노출하지 않는다.
- 목록 응답은 필요하면 `items`와 `pagination`을 가진 별도 response DTO로 감싼다.

```java
public record PolicySearchResponse(
        List<PolicyCardResponse> items,
        Pagination pagination,
        boolean degraded
) {
    public static PolicySearchResponse from(PolicySearchResult result) {
        return new PolicySearchResponse(result.items(), result.pagination(), result.degraded());
    }
}
```

## 6. Validation 규칙

- `spring-boot-starter-validation`을 사용한다.
- 문자열 필드는 보통 `@NotBlank`를 사용한다. `@NotNull`만으로는 공백 문자열을 막을 수 없다.
- 길이 제한은 `@Size`를 사용한다.
- 허용값이 제한된 문자열은 `@Pattern`을 사용한다.
- 숫자 범위는 `@Min`, `@Max`, `@Positive`를 사용한다.
- 중첩 DTO는 필드에도 `@Valid`를 붙인다.

Validation 실패는 `GlobalExceptionHandler`에서 `ErrorCode.INVALID_INPUT_VALUE`(`C001`) 형식으로 통일한다.

검증 오류 응답 계약은 프론트가 에러코드를 기준으로 메시지를 매핑할 수 있도록 아래 형태를 유지한다.

```json
{
  "code": "C001",
  "message": "입력 형태가 올바르지 않습니다.",
  "details": [
    {
      "field": "mode",
      "value": "INVALID",
      "reason": "mode는 FULL 또는 DELTA만 허용됩니다."
    }
  ]
}
```

- Bean Validation 실패(`MethodArgumentNotValidException`, `ConstraintViolationException`)는 백엔드에서 처리하고 HTTP 400 + `C001`로 내려준다.
- 프론트가 `code`를 안정적으로 확인할 수 있도록 validation 오류에서도 항상 `code`, `message`, `details` 필드를 유지한다.
- `details[].reason`에는 사용자에게 보여줄 수 있는 검증 메시지를 넣되, 내부 exception class, SQL, stack trace, secret 값은 넣지 않는다.
- Controller나 Service에서 validation 실패를 문자열 응답으로 직접 만들지 않는다.

## 7. 예외 처리 규칙

공통 예외 처리는 `global.error` 패키지에 둔다. 예외 응답은 참고 레포(`SpringBootStudy`) 방식처럼 `ErrorCode` enum을 기준으로 생성한다.

```text
global.error
├── ErrorCode.java
├── CustomException.java
├── ConflictException.java
├── ResourceNotFoundException.java
├── ErrorResponse.java
└── GlobalExceptionHandler.java
```

`ErrorCode`는 HTTP 상태, 서비스 코드, 사용자 메시지를 함께 가진다.

```java
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력 형태가 올바르지 않습니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "일치하는 정책이 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부에 예기치 않은 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

- 비즈니스 예외는 `throw new CustomException(ErrorCode.X)` 또는 이를 상속한 구체 예외로 던진다.
- 존재하지 않는 리소스는 `ResourceNotFoundException(ErrorCode.X_NOT_FOUND)`를 사용한다.
- 상태 충돌, 중복 실행, 중복 등록 등은 `ConflictException(ErrorCode.X_CONFLICT)`를 사용한다.
- Service에서 `throw new RuntimeException("...")` 형태로 비즈니스 예외를 직접 던지지 않는다.
- Controller에서 `try-catch`로 에러 응답을 만들지 않는다.
- 내부 예외 메시지, SQL, stack trace, secret 값을 응답에 노출하지 않는다.
- validation field error는 `field`, `value`, `reason` detail을 포함한다.
- 프론트가 사용자 메시지를 일관되게 매핑할 수 있도록 모든 예외 응답은 `code`를 필수로 포함한다.
- 새 `ErrorCode`를 추가할 때는 코드 체계와 사용자 메시지를 함께 정의하고, 프론트에서 매핑해야 하는 코드인지 PR 본문에 명시한다.
- 같은 오류 상황에 HTTP status만 다르게 내려주거나, 같은 `code`에 서로 다른 의미를 부여하지 않는다.

## 8. Lombok 규칙

백엔드 레포는 Lombok을 사용한다. Lombok은 반복적인 생성자, getter, JPA 기본 생성자 보일러플레이트를 줄이는 용도로만 제한적으로 사용한다.

- 생성자 주입은 `final` 필드와 `@RequiredArgsConstructor`를 기본으로 사용한다.
- JPA Entity에는 `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용할 수 있다.
- 예외 코드 enum, 커스텀 예외처럼 단순 getter만 필요한 타입에는 `@Getter`를 사용할 수 있다.
- DTO는 기존처럼 `record`를 우선 사용한다. DTO에 Lombok class를 도입해야 한다면 validation, 생성자, 불변성을 명확히 유지한다.
- `@Value` 주입 파라미터가 있는 생성자처럼 Lombok 적용이 오히려 의도를 흐리는 경우에는 명시적 생성자를 유지한다.

다음은 금지한다.

- `@Data`
- Entity의 `@Setter`
- Entity의 `@AllArgsConstructor`
- 양방향 연관관계 Entity의 무분별한 `@ToString`
- Entity의 무분별한 `@EqualsAndHashCode`
- Entity의 public all-args constructor

## 9. Entity / JPA 규칙

- Entity는 각 도메인의 `entity` 패키지에 둔다.
- Entity 기본 생성자는 JPA용으로만 열고 외부 생성을 제한한다.
- Entity를 API request/response에 직접 사용하지 않는다.
- 상태 변경은 setter 대신 의미 있는 메서드로 표현한다.
- 컬럼 제약은 `@Column(nullable = false, length = ...)`처럼 명시한다.
- Repository는 각 도메인의 `repository` 패키지에 interface로 둔다.
- 조회 결과가 없을 수 있으면 `Optional<T>`를 사용한다.

## 10. Service 규칙

- Service는 각 도메인의 `service` 패키지에 둔다.
- 읽기 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 생성/수정/삭제/외부 동기화는 명시적으로 `@Transactional`을 사용한다.
- 외부 API 호출, Redis lock, scheduler 등은 application boundary에서 흐름을 명확히 나눈다.
- secret, API key, token 값은 로그에 남기지 않는다.

## 11. Pageable 페이지네이션 규칙

목록 API는 직접 `page`, `size`를 받아 정규화하지 않는다. Spring Data `Pageable`/`Page`를 사용한다.

Controller 기준:

```java
@GetMapping
public ApiResponse<PolicyListResponse> searchPolicies(
        @PageableDefault(size = 20) Pageable pageable
) {
    return ApiResponse.ok(...);
}
```

Service / Repository 기준:

```java
@Transactional(readOnly = true)
public Page<Result> list(Pageable pageable) {
    return repository.findAll(pageable).map(Result::from);
}
```

max page size는 코드의 `Math.min`이 아니라 Spring 설정으로 제한한다.

```yaml
spring:
  data:
    web:
      pageable:
        max-page-size: 50
```

금지 패턴:

```java
@RequestParam(defaultValue = "0") int page
@RequestParam(defaultValue = "20") int size
int normalizedPage = Math.max(page, 0);
int normalizedSize = Math.min(size, 50);
int totalPages = (int) Math.ceil(...);
```

## 12. Redis / Lock / Scheduler 규칙

- 중복 실행을 막아야 하는 작업은 Redis lock을 사용한다.
- Lock은 owner token과 TTL을 가진다.
- Lock release 시 owner token을 확인한다.
- Scheduler는 기본적으로 비활성화 가능한 설정을 둔다.
- 외부 API sync job은 retry/backoff 설정을 둔다.
- interrupt 발생 시 interrupt status를 복구한다.

## 13. Docker / 실행 규칙

이 레포는 자체 `Dockerfile`과 `docker-compose.yml`을 소유한다. 별도 infra 레포나 루트 통합 Compose를 만들지 않는다.

```bash
docker compose up -d --build
```

Compose에는 백엔드, DB, Redis 실행 기준을 둔다. Docker/Compose 관련 변경 시 로컬에서 가능한 범위로 `docker compose config`, build, runtime smoke를 검증한다. 실행 환경에 Docker가 없으면 PR 검증 결과에 명시한다.

## 14. 테스트 규칙

- 기능 변경 시 관련 작은 테스트를 먼저 실행하고, 마지막에 전체 테스트를 실행한다.
- Controller validation은 MockMvc 테스트로 검증한다.
- Service 비즈니스 규칙은 Service 테스트로 검증한다.
- Repository/JPA 쿼리는 통합 테스트로 검증한다.
- PR 전 최소 `./gradlew test`를 통과시킨다.

```bash
export JAVA_HOME=/home/heeho3/.jdks/temurin-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test
```

## 15. API 검증 규칙

- 한글 query parameter를 curl로 검증할 때는 raw URL에 직접 넣지 말고 `--data-urlencode`를 사용한다.

```bash
curl -G http://localhost:8080/api/v1/policies \
  --data-urlencode 'keyword=월세' \
  --data-urlencode 'region=서울'
```

- actuator health와 API health를 구분한다.
- Redis나 DB가 꺼져 있으면 actuator health가 DOWN일 수 있다.

## 16. PR 전 체크리스트

- [ ] 도메인 단위 패키지 구조를 지켰다.
- [ ] Controller가 얇게 유지된다.
- [ ] 요청 DTO에 Bean Validation을 적용했다.
- [ ] `@RequestBody`에는 `@Valid`를 붙였다.
- [ ] Entity를 API 응답으로 직접 반환하지 않는다.
- [ ] Service에서 직접 `RuntimeException`을 던지지 않는다.
- [ ] 예외 응답은 `global.error`를 통해 통일된다.
- [ ] 검증 오류는 백엔드에서 `C001` 등 `ErrorCode` 기반 응답으로 처리되며, 프론트가 확인할 `code`가 포함된다.
- [ ] 새 에러코드를 추가했다면 프론트 메시지 매핑 필요 여부를 PR에 적었다.
- [ ] 목록 API는 `Pageable`/`Page`를 사용한다.
- [ ] 민감정보가 로그/응답에 노출되지 않는다.
- [ ] 작은 테스트와 전체 테스트를 실행했다.
- [ ] docs/API/기획 변경이 필요한 경우 docs 레포에 반영했다.
