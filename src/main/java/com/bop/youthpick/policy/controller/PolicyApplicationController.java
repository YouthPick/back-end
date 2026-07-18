package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationRegisterRequest;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * "정책 신청관리(관심→준비중→신청→완료)" HTTP 진입점. 비즈니스 판단은 전부 {@link PolicyApplicationService}에 위임하고, 여기선 요청
 * 파싱(Bean Validation) → 서비스 호출 → {@link PolicyApplicationResponse}로 응답 변환만 한다(api-design.md의
 * "Controller는 얇게" 규칙).
 */
@Validated
@RestController
@RequestMapping("/api/v1/policy-applications")
@RequiredArgsConstructor
public class PolicyApplicationController {

    private final PolicyApplicationService policyApplicationService;

    // POST /api/v1/policy-applications — "관심 등록" 액션의 진입점.
    // @Valid가 PolicyApplicationRegisterRequest의 Bean Validation(@NotNull, @Pattern 등)을 먼저 통과시킨 뒤에만
    // 이 메서드 본문이 실행된다.
    // status 문자열 → ApplicationStatus enum 변환은 parseStatus()를 거친다 — 평소엔 DTO의
    // @Pattern(PolicyApplicationRegisterRequest.java:12)이 걸러준 값만 들어오지만, 그 화이트리스트가
    // changeStatus()의 @Pattern과 별도로 중복 관리되는 탓에(ApplicationStatus.java 클래스 주석 참고) 어긋날 경우를
    // 대비해 parseStatus()가 500 대신 깔끔한 400(P007)으로 막아준다.
    // 신규 등록 vs soft-delete 재활성화 vs 중복 예외 판단은 PolicyApplicationService.register()가 전담한다.
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationResponse>> register(
            @CurrentUser Long userId,
            @Valid @RequestBody PolicyApplicationRegisterRequest request) {
        PolicyApplication application =
                policyApplicationService.register(
                        userId,
                        request.policyId(),
                        parseStatus(request.status()),
                        request.memo(),
                        request.endAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationResponse.from(application)));
    }

    // GET /api/v1/policy-applications — 내 정책 신청 목록 페이지 조회.
    // page/size/totalPages를 컨트롤러가 직접 계산하지 않는다 — @PageableDefault(size=20)로 Spring Data가 Pageable을
    // 만들고,
    // PolicyApplicationService.getApplications()가 이미 PolicyApplicationResponse로 변환된 Page를 돌려주므로
    // 여기선 그 Page를 (content, meta) 형태로 ApiResponse에 담기만 한다(api-design.md의 Pageable 규칙).
    @GetMapping
    public ApiResponse<List<PolicyApplicationResponse>> getApplications(
            @CurrentUser Long userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyApplicationResponse> page =
                policyApplicationService.getApplications(userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    // PATCH /api/v1/policy-applications/{id}/status — 상태만 단독으로 바꾸는 엔드포인트.
    // register()의 DTO와 같은 4개 상태값 화이트리스트를 여기서도 별도로 유지한다(위 register() 주석 참고 — 드리프트 위험 동일,
    // parseStatus()가 같은 방식으로 방어한다).
    // 소유권 검증은 여기가 아니라 PolicyApplicationService.changeStatus() 안의 application.verifyOwner()에서 한다 —
    // id로 조회한 신청이 진짜 이 userId 소유인지는 서비스 계층 책임이다.
    @PatchMapping("/{id}/status")
    public ApiResponse<PolicyApplicationResponse> changeStatus(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam
                    @NotBlank(message = "상태는 필수입니다.")
                    @Pattern(
                            regexp = "INTERESTED|PREPARING|APPLIED|COMPLETED",
                            message = "유효하지 않은 상태값입니다.")
                    String status) {
        PolicyApplication application =
                policyApplicationService.changeStatus(id, userId, parseStatus(status));
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    // PATCH /api/v1/policy-applications/{id}/memo — 메모만 단독 수정.
    // 컨트롤러는 @Size로 길이만 검증하고, 빈 문자열/공백을 null로 통일하는 정규화는
    // PolicyApplicationService.blankToNull()(서비스 계층)이 담당한다 — 여기서 값을 가공하지 않는다.
    @PatchMapping("/{id}/memo")
    public ApiResponse<PolicyApplicationResponse> updateMemo(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam @Size(max = 2000, message = "메모는 2000자를 초과할 수 없습니다.") String memo) {
        PolicyApplication application = policyApplicationService.updateMemo(id, userId, memo);
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    /**
     * memo와 달리 endAt은 생략 시 필수 에러가 아니라 마감일 초기화(clear)로 동작한다. LocalDateTime처럼 String이 아닌 타입은 Spring이
     * "파라미터 생략"과 "빈 문자열(endAt=)"을 바인딩 단계에서 이미 null로 합쳐 버려 required=true로는 이 둘을 구분할 수 없다(둘 다 "필수값
     * 없음" 에러가 됨) — 그래서 memo처럼 필수로 강제하지 않고 의도적으로 생략=초기화로 둔다. 정책 마감일을 넘는지 검증하는 로직은
     * PolicyApplicationService.updateEndAt() → resolveEndAt()/validateWithinPolicyDeadline()에 있다.
     */
    @PatchMapping("/{id}/end-at")
    public ApiResponse<PolicyApplicationResponse> updateEndAt(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endAt) {
        PolicyApplication application = policyApplicationService.updateEndAt(id, userId, endAt);
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    // DELETE /api/v1/policy-applications/{id} — 소프트 삭제(관심 해제).
    // PolicyApplicationService.delete() → PolicyApplication.delete()가 deletedAt만 세팅한다(물리 삭제 아님, 재등록
    // 시 reactivate()로 되살아남).
    // 반환할 데이터가 없어 ApiResponse.ok(null) — data는 null, meta 없음.
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long id) {
        policyApplicationService.delete(id, userId);
        return ApiResponse.ok(null);
    }

    // register()/changeStatus() 공통 — ApplicationStatus.valueOf()를 직접 부르지 않고 이걸 거친다.
    // 평소엔 @Pattern이 이미 걸러준 값만 들어와서 아무 차이가 없지만, 두 @Pattern과 enum 상수가 어긋나는 드리프트가
    // 생기면 IllegalArgumentException을 여기서 CustomException(P007)으로 바꿔, 처리 못 한 예외로 새서 500이 되는 걸 막는다.
    private ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(PolicyErrorCode.INVALID_APPLICATION_STATUS);
        }
    }
}
