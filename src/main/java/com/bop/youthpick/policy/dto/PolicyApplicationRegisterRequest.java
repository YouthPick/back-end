package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * PolicyApplicationController.register()가 {@code @Valid @RequestBody}로 바인딩하는 요청 DTO. 검증을 통과한 필드들이
 * 그대로 PolicyApplicationService.register()의 인자로 넘어간다(policyId, status(문자열→enum 변환은 컨트롤러의
 * parseStatus()에서), memo, endAt 순서 그대로).
 */
public record PolicyApplicationRegisterRequest(
        // PolicyApplicationService.register()에서 policyRepository.findById(policyId)로 조회된다 —
        // 없으면 PolicyErrorCode.POLICY_NOT_FOUND.
        @NotNull(message = "정책 ID는 필수입니다.") Long policyId,
        // enum이 아니라 문자열 + @Pattern인 이유: enum을 직접 바인딩하면 Jackson 역직렬화 단계에서 실패해 C001로 못
        // 내려간다(api-design.md).
        // 이 화이트리스트 문자열은 ApplicationStatus enum 상수 이름과 정확히 일치해야 하고(ApplicationStatus.java 참고),
        // PolicyApplicationController.changeStatus()의 @Pattern과도 중복 관리된다. 이 화이트리스트와 enum이
        // 어긋나도 컨트롤러의 parseStatus()가 IllegalArgumentException을
        // PolicyErrorCode.INVALID_APPLICATION_STATUS(P007)로 바꿔주므로 500까지 새지는 않는다.
        @NotBlank(message = "상태는 필수입니다.")
                @Pattern(
                        regexp = "INTERESTED|PREPARING|APPLIED|COMPLETED",
                        message = "유효하지 않은 상태값입니다.")
                String status,
        // PolicyApplicationService.blankToNull()이 공백/빈 문자열을 null로 통일한다 — 여기 @Size는 길이만 검증하고 트림은 하지
        // 않는다.
        @Size(max = 2000, message = "메모는 2000자를 초과할 수 없습니다.") String memo,
        // 생략(null) 시 PolicyApplicationService.resolveEndAt()이 정책 자체의 신청 마감일로 기본값을 채운다.
        LocalDateTime endAt) {}
