---
paths:
  - "src/main/java/**"
---

# Controller / DTO / 응답 규칙

## Controller

- 생성자 주입(`@RequiredArgsConstructor` + `final` 필드). 필드 주입(`@Autowired`) 금지.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 요청 body 파라미터에는 `@Valid @RequestBody`를 붙인다. validation 없는 요청 DTO, `@Valid` 없는 바인딩은 금지.
- 성공 응답은 항상 `global.common.ApiResponse`로 감싼다.

## 요청 DTO (`{domain}.dto`)

- `record`를 우선 사용한다. 모든 사용자 입력 필드에 의도를 드러내는 Bean Validation annotation을 붙인다.
- validation message는 한국어로 작성한다.
- 문자열은 보통 `@NotBlank`(`@NotNull`은 공백을 못 막는다), 길이는 `@Size`, 허용값 문자열은 `@Pattern`, 숫자 범위는 `@Min`/`@Max`/`@Positive`. 중첩 DTO 필드에는 `@Valid`.
- enum을 직접 바인딩하면 Jackson deserialization 단계에서 터진다. 잘못된 enum 값을 `C001` 입력값 오류로 내려야 하면 문자열 필드 + `@Pattern`을 사용한다.

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

## 응답 DTO

- Entity를 받아 정적 팩토리 메서드 `from(...)`으로 생성한다.
- Entity의 민감 필드, 내부 상태, raw payload, secret 값을 노출하지 않는다.

```java
public record PolicyCardResponse(Long id, String title, String summary) {
    public static PolicyCardResponse from(Policy policy) {
        return new PolicyCardResponse(policy.getId(), policy.getTitle(), policy.getSummary());
    }
}
```

## 공통 응답 봉투 `ApiResponse` (data + meta)

- 단일/객체 데이터는 `ApiResponse.ok(data)`. 페이지 등 부가정보(`page`, `totalCount` 등)는 **`data`가 아니라 `meta`에** 담는다.
- 200 외 상태 코드가 필요하면 `ResponseEntity<ApiResponse<?>>`로 감싼다.
- 에러 응답은 `ApiResponse`가 아니라 `global.error.ErrorResponse`로 내려간다 (→ `error-handling.md`).

## 페이지네이션 — Spring Data `Pageable`/`Page`

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

- max page size는 코드의 `Math.min`이 아니라 설정(`spring.data.web.pageable.max-page-size`)으로 제한한다.
- 금지: `@RequestParam int page/size` 직접 수신, `Math.min(size, 50)` 정규화, `totalPages` 직접 계산.
