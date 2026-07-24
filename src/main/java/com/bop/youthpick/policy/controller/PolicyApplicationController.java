package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationCreateRequest;
import com.bop.youthpick.policy.dto.PolicyApplicationEndAtUpdateRequest;
import com.bop.youthpick.policy.dto.PolicyApplicationMemoUpdateRequest;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "정책 신청관리")
@RestController
@RequestMapping("/api/v1/policy-applications")
@RequiredArgsConstructor
public class PolicyApplicationController {

    private final PolicyApplicationService policyApplicationService;

    /**
     * {@code POST /api/v1/policy-applications} — "관심 등록" 액션의 진입점. {@code status} 문자열 → {@link
     * ApplicationStatus} 변환은 {@link #parseStatus}를 거친다 — 평소엔 DTO의 {@code @Pattern}(={@link
     * ApplicationStatus#VALUES_PATTERN})이 걸러준 값만 들어오지만, 만에 하나 어긋날 경우를 대비해 {@link #parseStatus}가 500
     * 대신 깔끔한 400(P007)으로 막아준다. 신규 등록 vs soft-delete 재활성화 vs 중복 예외 판단은 {@link
     * PolicyApplicationService#create}가 전담한다.
     */
    @Operation(
            summary = "정책 신청 관심 등록",
            description =
                    "POST /api/v1/policy-applications — \"관심 등록\" 액션의 진입점. status 문자열 → ApplicationStatus 변환은"
                            + " parseStatus를 거친다 — 평소엔 DTO의 @Pattern(=ApplicationStatus.VALUES_PATTERN)이 걸러준 값만 들어오지만, 만에 하나"
                            + " 어긋날 경우를 대비해 parseStatus가 500 대신 깔끔한 400(P007)으로 막아준다. 신규 등록 vs soft-delete 재활성화 vs 중복 예외"
                            + " 판단은 PolicyApplicationService.create가 전담한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationResponse>> create(
            @CurrentUser Long userId, @Valid @RequestBody PolicyApplicationCreateRequest request) {
        PolicyApplication application =
                policyApplicationService.create(
                        userId,
                        request.policyId(),
                        parseStatus(request.status()),
                        request.memo(),
                        request.endAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationResponse.from(application)));
    }

    /**
     * {@code GET /api/v1/policy-applications} — 내 정책 신청 목록 페이지 조회. page/size/totalPages는 컨트롤러가 직접
     * 계산하지 않는다 — {@link PolicyApplicationService#getApplications}가 이미 {@link
     * PolicyApplicationResponse}로 변환된 {@code Page}를 돌려주므로, 여기선 그 Page를 (content, meta) 형태로 {@link
     * ApiResponse}에 담기만 한다(api-design.md의 Pageable 규칙).
     */
    @Operation(
            summary = "정책 신청 목록 조회",
            description =
                    "GET /api/v1/policy-applications — 내 정책 신청 목록 페이지 조회. page/size/totalPages는 컨트롤러가 직접"
                            + " 계산하지 않는다 — PolicyApplicationService.getApplications가 이미 PolicyApplicationResponse로 변환된"
                            + " Page를 돌려주므로, 여기선 그 Page를 (content, meta) 형태로 ApiResponse에 담기만 한다.")
    @GetMapping
    public ApiResponse<List<PolicyApplicationResponse>> getApplications(
            @CurrentUser Long userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyApplicationResponse> page =
                policyApplicationService.getApplications(userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    /**
     * {@code PATCH /api/v1/policy-applications/{id}/status} — 상태만 단독으로 바꾸는 엔드포인트. {@link #create}의
     * DTO와 같은 {@link ApplicationStatus#VALUES_PATTERN} 화이트리스트를 공유한다({@link #parseStatus}가 방어선 역할은
     * 동일하게 유지). 소유권 검증은 여기가 아니라 {@link PolicyApplicationService#changeStatus} 안의 {@code
     * verifyOwner()}에서 한다 — id로 조회한 신청이 진짜 이 userId 소유인지는 서비스 계층 책임이다.
     */
    @Operation(
            summary = "정책 신청 상태 변경",
            description =
                    "PATCH /api/v1/policy-applications/{id}/status — 상태만 단독으로 바꾸는 엔드포인트. create의 DTO와 같은"
                            + " ApplicationStatus.VALUES_PATTERN 화이트리스트를 공유한다(parseStatus가 방어선 역할은 동일하게 유지). 소유권 검증은 여기가"
                            + " 아니라 PolicyApplicationService.changeStatus 안의 verifyOwner()에서 한다 — id로 조회한 신청이 진짜 이 userId"
                            + " 소유인지는 서비스 계층 책임이다.")
    @PatchMapping("/{id}/status")
    public ApiResponse<PolicyApplicationResponse> changeStatus(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam
                    @NotBlank(message = "상태는 필수입니다.")
                    @Pattern(regexp = ApplicationStatus.VALUES_PATTERN, message = "유효하지 않은 상태값입니다.")
                    String status) {
        PolicyApplication application =
                policyApplicationService.changeStatus(id, userId, parseStatus(status));
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    /**
     * {@code PATCH /api/v1/policy-applications/{id}/memo} — 메모만 단독 수정. 컨트롤러는 {@code @Size}로 DTO 필드
     * 길이를 검증하고, 빈 문자열/공백을 null로 통일하는 정규화는 서비스 계층이 담당한다.
     */
    @Operation(
            summary = "정책 신청 메모 수정",
            description =
                    "PATCH /api/v1/policy-applications/{id}/memo — 메모만 단독 수정. 컨트롤러는 @Size로 DTO 필드 길이를"
                            + " 검증하고, 빈 문자열/공백을 null로 통일하는 정규화는 서비스 계층이 담당한다.")
    @PatchMapping("/{id}/memo")
    public ApiResponse<PolicyApplicationResponse> updateMemo(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PolicyApplicationMemoUpdateRequest request) {
        PolicyApplication application =
                policyApplicationService.updateMemo(id, userId, request.memo());
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    /** {@code PATCH /api/v1/policy-applications/{id}/end-at} — 마감일 단독 수정. */
    @Operation(
            summary = "정책 신청 마감일 수정",
            description = "PATCH /api/v1/policy-applications/{id}/end-at — 마감일 단독 수정.")
    @PatchMapping("/{id}/end-at")
    public ApiResponse<PolicyApplicationResponse> updateEndAt(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PolicyApplicationEndAtUpdateRequest request) {
        PolicyApplication application =
                policyApplicationService.updateEndAt(id, userId, request.endAt());
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    /**
     * {@code DELETE /api/v1/policy-applications/{id}} — 소프트 삭제(관심 해제). 물리 삭제가 아니라 {@code
     * deletedAt}만 세팅한다(재등록 시 reactivate로 되살아남). 반환할 데이터가 없어 {@code ApiResponse.ok(null)} — data는
     * null, meta 없음.
     */
    @Operation(
            summary = "정책 신청 관심 해제",
            description =
                    "DELETE /api/v1/policy-applications/{id} — 소프트 삭제(관심 해제). 물리 삭제가 아니라 deletedAt만 세팅한다(재등록"
                            + " 시 reactivate로 되살아남). 반환할 데이터가 없어 ApiResponse.ok(null) — data는 null, meta 없음.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long id) {
        policyApplicationService.delete(id, userId);
        return ApiResponse.ok(null);
    }

    /**
     * {@link #create}/{@link #changeStatus} 공통 — {@code ApplicationStatus.valueOf()}를 직접 부르지 않고 이걸
     * 거친다. 평소엔 {@code @Pattern}(={@link ApplicationStatus#VALUES_PATTERN})이 이미 걸러준 값만 들어와서 아무 차이가
     * 없지만, 그 문자열 상수와 enum 상수가 어긋나는 드리프트가 생기면 {@code IllegalArgumentException}을 여기서 {@code
     * CustomException(P007)}로 바꿔, 처리 못 한 예외로 새서 500이 되는 걸 막는다.
     */
    private ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(PolicyErrorCode.INVALID_APPLICATION_STATUS);
        }
    }
}
