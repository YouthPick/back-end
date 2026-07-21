package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * PolicyApplicationController.create()가 {@code @Valid @RequestBody}로 바인딩하는 요청 DTO. 검증을 통과한 필드들이 그대로
 * PolicyApplicationService.create()의 인자로 넘어간다(policyId, status(문자열→enum 변환은 컨트롤러의 parseStatus()에서),
 * memo, endAt 순서 그대로).
 *
 * @param policyId PolicyApplicationService.create()에서 policyRepository.findById(policyId)로 조회된다 —
 *     없으면 PolicyErrorCode.POLICY_NOT_FOUND.
 * @param status enum이 아니라 문자열 + {@code @Pattern}인 이유: enum을 직접 바인딩하면 Jackson 역직렬화 단계에서 실패해 C001로 못
 *     내려간다(api-design.md). {@link ApplicationStatus#VALUES_PATTERN}을 참조해
 *     PolicyApplicationController.changeStatus()의 {@code @Pattern}과 같은 화이트리스트를 공유한다. 이 화이트리스트와
 *     enum이 어긋나도 컨트롤러의 parseStatus()가 IllegalArgumentException을
 *     PolicyErrorCode.INVALID_APPLICATION_STATUS(P007)로 바꿔주므로 500까지 새지는 않는다.
 * @param memo PolicyApplicationService.blankToNull()이 공백/빈 문자열을 null로 통일한다 — 여기 {@code @Size}는 길이만
 *     검증하고 트림은 하지 않는다.
 * @param endAt 생략(null) 시 PolicyApplicationService.resolveEndAt()이 정책 자체의 신청 마감일로 기본값을 채운다.
 */
public record PolicyApplicationCreateRequest(
        @NotNull(message = "정책 ID는 필수입니다.") Long policyId,
        @NotBlank(message = "상태는 필수입니다.")
                @Pattern(regexp = ApplicationStatus.VALUES_PATTERN, message = "유효하지 않은 상태값입니다.")
                String status,
        @Size(max = 2000, message = "메모는 2000자를 초과할 수 없습니다.") String memo,
        LocalDateTime endAt) {}
