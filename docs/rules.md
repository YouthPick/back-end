# YouthPick Back-end Rules

이 문서는 `youth-pick`에서 작업하는 모든 에이전트와 개발자가 따라야 하는 백엔드 구현 규칙이다. 새 기능을 만들거나 기존 코드를 수정하기 전에 반드시 이 파일을 먼저 읽고, 이 규칙과 충돌하는 구현은 하지 않는다.

> Git 브랜치/커밋/PR 규칙은 [`git-convention.md`](./git-convention.md)를 따른다.

## 1. 작업 흐름

- 기획 변경이나 구현 계획이 필요한 작업은 먼저 계획을 정리하고 관련 GitHub Issue를 만든 뒤 작업한다.
- 백엔드 작업은 issue 번호가 포함된 브랜치에서 진행한다. ([`git-convention.md`](./git-convention.md))
- 구현 후에는 테스트를 직접 실행하고, PR에 실제 검증 결과를 적는다.
- 단순 설명이나 스텁으로 끝내지 않는다. 동작하는 코드와 검증 결과를 남긴다.

## 2. 기술 기준

- **Java 21**을 사용한다.
- **Spring Boot 3.5.16** 기준으로 작성한다.
- 기본 스택: Spring Web MVC, Spring Data JPA, Spring Data Redis(Spring Session 저장소), Spring Security, Bean Validation, Spring Boot Actuator.
- DB: 로컬/테스트는 **H2 in-memory(MySQL 호환 모드)**, 실배포는 **MySQL**. 프로파일과 환경변수로 전환한다.
- 빌드는 **Gradle Wrapper (Groovy DSL, `build.gradle`)** 를 사용한다. Java toolchain이 JDK 21을 자동 프로비저닝하므로 `JAVA_HOME`을 수동으로 지정할 필요는 없다.
- 테스트 실행:

  ```powershell
  # Windows (PowerShell)
  .\gradlew.bat test
  ```

  ```bash
  # macOS / Linux / CI
  ./gradlew test
  ```

## 3. 패키지 구조

패키지는 도메인 단위 폴더 구조로 만든다. 다만 도메인 내부는 과하게 DDD식으로 나누지 않고 `controller`, `service`, `repository`, `dto`, `entity` 중심의 단순 구조를 사용한다.

현재 기준 구조는 다음과 같다.

```text
com.bop.youthpick
├── global
│   ├── common      // ApiResponse 등 공통 응답 봉투
│   ├── config      // SecurityConfig, RestAuthenticationEntryPoint 등
│   └── error       // ErrorCode, CustomException, GlobalExceptionHandler 등
├── policy
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
└── user
    ├── controller
    ├── service
    ├── repository
    ├── dto
    └── entity
```

새 도메인(예: `favorite`, 자가진단 등)을 추가할 때는 아래 형태를 따른다.

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
@RequiredArgsConstructor
public class PolicyController {

    private final PolicySearchService policySearchService;
}
```

- 생성자 주입을 사용한다(`@RequiredArgsConstructor` + `final` 필드).
- 필드 주입(`@Autowired` field injection)은 금지한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 요청 body를 받는 파라미터에는 `@Valid @RequestBody`를 붙인다.
- 성공 응답은 항상 `global.common.ApiResponse`로 감싼다(§5.3 참고).

## 5. DTO / 응답 규칙

요청 DTO와 응답 DTO는 각 도메인의 `dto` 패키지에 둔다. DTO는 API 계약을 표현하는 객체이며 Entity를 외부에 노출하지 않기 위한 경계다.

### 5.1 요청 DTO

- 요청 DTO는 `record`를 사용할 수 있다. 다만 안정적인 field-level validation 응답이 필요한 경우 필드에 Bean Validation annotation을 명확히 붙인다.
- enum 값을 직접 바인딩하면 Jackson deserialization 단계에서 터질 수 있다. 잘못된 enum 값을 `C001` 입력값 오류로 내려야 하면 문자열 필드 + `@Pattern`을 사용한다.
- 모든 사용자 입력 필드는 의도를 드러내는 validation annotation을 붙인다.
- validation message는 한국어로 작성한다.

```java
public record UserSignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
```

요청 DTO에 validation이 없거나 Controller에서 `@Valid` 없이 받는 코드는 금지한다.

### 5.2 응답 DTO

- 응답 DTO는 Entity를 받아 정적 팩토리 메서드 `from(...)`으로 생성한다.
- 응답 DTO에서 Entity의 민감 필드, 내부 상태, raw payload, secret 값을 노출하지 않는다.

```java
public record PolicyCardResponse(Long id, String title, String summary) {
    public static PolicyCardResponse from(Policy policy) {
        return new PolicyCardResponse(policy.getId(), policy.getTitle(), policy.getSummary());
    }
}
```

### 5.3 공통 응답 봉투 (`ApiResponse`)

성공 응답은 `global.common.ApiResponse<T>`로 감싼다. 이 프로젝트의 `ApiResponse`는 `data` + `meta` 구조다.

```java
public record ApiResponse<T>(T data, Map<String, Object> meta) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) { ... }
}
```

- 단일/객체 데이터는 `ApiResponse.ok(data)`.
- 페이지네이션 등 부가정보(`page`, `totalCount` 등)는 **`data`가 아니라 `meta`에 담는다.**
- 200 외 상태 코드가 필요하면 `ResponseEntity<ApiResponse<?>>`로 감싼다.
- 에러 응답은 `ApiResponse`가 아니라 `global.error.ErrorResponse` 형식으로 내려간다(§7).

```java
@GetMapping
public ApiResponse<List<PolicyCardResponse>> search(@PageableDefault(size = 20) Pageable pageable) {
    Page<PolicyCardResponse> page = policySearchService.search(pageable);
    return ApiResponse.ok(page.getContent(), Map.of(
            "page", page.getNumber(),
            "totalCount", page.getTotalElements(),
            "totalPages", page.getTotalPages()
    ));
}
```

## 6. Validation 규칙

- `spring-boot-starter-validation`을 사용한다.
- 문자열 필드는 보통 `@NotBlank`를 사용한다. `@NotNull`만으로는 공백 문자열을 막을 수 없다.
- 길이 제한은 `@Size`, 허용값 제한 문자열은 `@Pattern`, 숫자 범위는 `@Min`/`@Max`/`@Positive`를 사용한다.
- 중첩 DTO는 필드에도 `@Valid`를 붙인다.

Validation 실패는 `GlobalExceptionHandler`에서 `ErrorCode.INVALID_INPUT_VALUE`(`C001`) 형식으로 통일한다. 프론트가 에러코드를 기준으로 메시지를 매핑할 수 있도록 아래 형태를 유지한다.

```json
{
  "code": "C001",
  "message": "입력 형태가 올바르지 않습니다.",
  "details": [
    { "field": "username", "value": "", "reason": "아이디는 필수입니다." }
  ]
}
```

- Bean Validation 실패(`MethodArgumentNotValidException`, `ConstraintViolationException`)는 백엔드에서 처리하고 HTTP 400 + `C001`로 내려준다.
- validation 오류에서도 항상 `code`, `message`, `details` 필드를 유지한다.
- `details[].reason`에는 사용자에게 보여줄 수 있는 검증 메시지만 넣고, 내부 exception class/SQL/stack trace/secret 값은 넣지 않는다.
- Controller나 Service에서 validation 실패를 문자열 응답으로 직접 만들지 않는다.

## 7. 예외 처리 규칙

공통 예외 처리는 `global.error` 패키지에 둔다. 예외 응답은 `ErrorCode` enum을 기준으로 생성한다.

```text
global.error
├── ErrorCode.java
├── CustomException.java
├── ConflictException.java
├── ResourceNotFoundException.java
├── ErrorResponse.java
└── GlobalExceptionHandler.java
```

`ErrorCode`는 HTTP 상태, 서비스 코드, 사용자 메시지를 함께 가진다. **서비스 코드는 접두어 + 3자리** 규칙을 따른다.

| 접두어 | 영역 |
|--------|------|
| `C` | 공통(common) |
| `A` | 인증/인가(auth) |
| `S` | 서버(server) |
| `U` | 사용자(user) |
| `P` | 정책(policy) |
| `F` | 즐겨찾기(favorite) |
| `D` | 자가진단(diagnosis) |

```java
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부에 예기치 않은 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력 형태가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "일치하는 정책이 존재하지 않습니다.");
    // ...
}
```

- 비즈니스 예외는 `throw new CustomException(ErrorCode.X)` 또는 이를 상속한 구체 예외로 던진다.
- 존재하지 않는 리소스는 `ResourceNotFoundException(ErrorCode.X_NOT_FOUND)`를 사용한다.
- 상태 충돌, 중복 실행, 중복 등록 등은 `ConflictException(ErrorCode.X_CONFLICT)`를 사용한다.
- Service에서 `throw new RuntimeException("...")` 형태로 비즈니스 예외를 직접 던지지 않는다.
- Controller에서 `try-catch`로 에러 응답을 만들지 않는다.
- 내부 예외 메시지/SQL/stack trace/secret 값을 응답에 노출하지 않는다.
- 새 `ErrorCode`를 추가할 때는 접두어 체계를 지키고 사용자 메시지를 함께 정의하며, 프론트에서 매핑해야 하는 코드인지 PR 본문에 명시한다.
- 같은 오류 상황에 HTTP status만 다르게 내려주거나, 같은 `code`에 서로 다른 의미를 부여하지 않는다.

## 8. Lombok 규칙

Lombok은 반복적인 생성자, getter, JPA 기본 생성자 보일러플레이트를 줄이는 용도로만 제한적으로 사용한다.

- 생성자 주입은 `final` 필드와 `@RequiredArgsConstructor`를 기본으로 사용한다.
- JPA Entity에는 `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용할 수 있다.
- 예외 코드 enum, 커스텀 예외처럼 단순 getter만 필요한 타입에는 `@Getter`를 사용할 수 있다.
- DTO는 `record`를 우선 사용한다.

다음은 금지한다.

- `@Data`
- Entity의 `@Setter`
- Entity의 `@AllArgsConstructor` / public all-args constructor
- 양방향 연관관계 Entity의 무분별한 `@ToString`
- Entity의 무분별한 `@EqualsAndHashCode`

## 9. Entity / JPA 규칙

- Entity는 각 도메인의 `entity` 패키지에 둔다.
- Entity 기본 생성자는 JPA용으로만 열고 외부 생성을 제한한다(`@NoArgsConstructor(access = AccessLevel.PROTECTED)`).
- Entity를 API request/response에 직접 사용하지 않는다.
- 상태 변경은 setter 대신 의미 있는 메서드로 표현한다.
- 컬럼 제약은 `@Column(nullable = false, length = ...)`처럼 명시한다.
- Repository는 각 도메인의 `repository` 패키지에 interface로 둔다.
- 조회 결과가 없을 수 있으면 `Optional<T>`를 사용한다.
- `ddl-auto`는 로컬 `update`, 테스트 `create-drop`. 운영에서 `create`/`update`를 무분별하게 쓰지 않는다.

## 10. Service 규칙

- Service는 각 도메인의 `service` 패키지에 둔다.
- 읽기 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 생성/수정/삭제/외부 동기화는 명시적으로 `@Transactional`을 사용한다.
- 외부 API 호출, scheduler 등은 흐름을 명확히 나눈다.
- secret, API key, OAuth token 값은 로그에 남기지 않는다.

## 11. Pageable 페이지네이션 규칙

목록 API는 직접 `page`, `size`를 받아 정규화하지 않는다. Spring Data `Pageable`/`Page`를 사용한다.

```java
@GetMapping
public ApiResponse<List<PolicyCardResponse>> search(@PageableDefault(size = 20) Pageable pageable) {
    Page<PolicyCardResponse> page = policySearchService.search(pageable);
    return ApiResponse.ok(page.getContent(), Map.of(
            "page", page.getNumber(),
            "totalCount", page.getTotalElements(),
            "totalPages", page.getTotalPages()
    ));
}
```

```java
@Transactional(readOnly = true)
public Page<PolicyCardResponse> search(Pageable pageable) {
    return repository.findAll(pageable).map(PolicyCardResponse::from);
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
int normalizedSize = Math.min(size, 50);
int totalPages = (int) Math.ceil(...);
```

## 12. 인증 / 세션 / Redis 규칙

이 프로젝트는 **Spring Security 세션 기반 인증** + **Spring Session(Redis 저장소)** + **OAuth 소셜 로그인(Google / Naver / Kakao)** 구조다.

- 세션은 Spring Session을 통해 Redis에 저장한다(`spring-boot-starter-data-redis` + `spring-session-data-redis`). in-memory 세션에 의존하는 코드를 만들지 않는다.
- 인증 실패(미인증 접근)는 리다이렉트가 아니라 `RestAuthenticationEntryPoint`에서 JSON + `A001 UNAUTHORIZED`로 내려준다.
- OAuth client id/secret 등 secret 값은 코드에 하드코딩하지 않고 환경변수(`GOOGLE_OAUTH_CLIENT_ID` 등)로 주입한다. `.env`는 커밋하지 않는다.
- 인가 규칙(경로별 권한)은 `SecurityConfig`에서 관리하고 Controller에 흩뿌리지 않는다.
- 중복 실행을 막아야 하는 배치/스케줄 작업을 도입한다면 Redis lock(owner token + TTL, release 시 owner 확인)을 사용하고, scheduler는 설정으로 on/off 가능하게 둔다. (현재 도입 전이면 도입 시 이 규칙을 따른다.)

## 13. 실행 / Docker 규칙

- **앱(백엔드)은 컨테이너화하지 않고 로컬 IDE / Gradle로 실행한다.** `docker-compose.yml`은 로컬 인프라(Redis, 필요 시 MySQL)만 띄운다.
- 로컬 인프라 기동:

  ```bash
  docker compose up -d      # Redis (필요 시 MySQL) 기동
  docker compose ps
  docker compose down       # 중지 (볼륨 데이터 보존)
  docker compose down -v    # 중지 + 볼륨 삭제(초기화)
  ```

- 로컬 기본 DB는 H2 in-memory이므로 MySQL 없이도 앱이 뜬다. MySQL로 붙일 때만 compose의 MySQL 서비스와 관련 환경변수를 활성화한다.
- Compose/인프라 관련 변경 시 로컬에서 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. 실행 환경에 Docker가 없으면 PR 검증 결과에 명시한다.

## 14. 테스트 규칙

- 기능 변경 시 관련 작은 테스트를 먼저 실행하고, 마지막에 전체 테스트를 실행한다.
- Controller validation은 MockMvc 테스트로 검증한다.
- Service 비즈니스 규칙은 Service 테스트로 검증한다.
- Repository/JPA 쿼리는 통합 테스트로 검증한다(테스트 프로파일은 H2 `create-drop`).
- PR 전 최소 `./gradlew test`(Windows는 `.\gradlew.bat test`)를 통과시킨다.

## 15. API 검증 규칙

- 한글 query parameter를 curl로 검증할 때는 raw URL에 직접 넣지 말고 `--data-urlencode`를 사용한다.

  ```bash
  curl -G http://localhost:8080/api/v1/policies \
    --data-urlencode 'keyword=월세' \
    --data-urlencode 'region=서울'
  ```

- actuator health(`/actuator/health`)와 API health를 구분한다.
- Redis나 DB가 꺼져 있으면 actuator health가 DOWN일 수 있다.

## 16. PR 전 체크리스트

- [ ] 도메인 단위 패키지 구조를 지켰다.
- [ ] Controller가 얇게 유지된다.
- [ ] 요청 DTO에 Bean Validation을 적용했다.
- [ ] `@RequestBody`에는 `@Valid`를 붙였다.
- [ ] Entity를 API 응답으로 직접 반환하지 않는다.
- [ ] 성공 응답을 `ApiResponse`로 감쌌고, 페이지 정보는 `meta`에 담았다.
- [ ] Service에서 직접 `RuntimeException`을 던지지 않는다.
- [ ] 예외 응답은 `global.error`를 통해 통일되고, `code`(접두어 규칙)를 포함한다.
- [ ] 새 에러코드를 추가했다면 프론트 메시지 매핑 필요 여부를 PR에 적었다.
- [ ] 목록 API는 `Pageable`/`Page`를 사용한다.
- [ ] secret/OAuth token/민감정보가 로그·응답에 노출되지 않는다.
- [ ] 작은 테스트와 전체 테스트를 실행했다.