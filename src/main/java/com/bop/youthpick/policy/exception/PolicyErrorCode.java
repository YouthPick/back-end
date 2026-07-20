package com.bop.youthpick.policy.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements ErrorCode {
    // PolicyApplicationService.register()에서 policyRepository.findById(policyId)가 비었을 때 던진다.
    POLICY_NOT_FOUND("P001", "일치하는 정책이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    // PolicyApplicationService.register()에서 두 번 쓰인다: ①활성 상태인 기존 신청이 있을 때(중복 등록 시도),
    // ②동시 요청으로 UNIQUE(user, policy) 제약을 위반한 DataIntegrityViolationException을 잡았을 때 — 같은 도메인 에러로 통일.
    POLICY_ALREADY_EXISTS("P002", "이미 존재하는 정책입니다.", HttpStatus.CONFLICT),
    // policy-application 기능이 아니라 user 도메인의 OnboardingService에서 쓰인다(관심 지역 코드 검증) —
    // "정책" 접두어(P)를 쓰지만 실제로는 지역 코드 조회 실패에 재사용되는 코드다.
    REGION_NOT_FOUND("P003", "존재하지 않는 지역 코드입니다.", HttpStatus.NOT_FOUND),
    // PolicyApplicationService.findActive(id)가 못 찾을 때(changeStatus/updateMemo/updateEndAt/delete 공통
    // 경로),
    // PolicyApplicationChecklistService에서도 같은 목적으로 재사용된다.
    POLICY_APPLICATION_NOT_FOUND("P004", "정책 관리 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    // PolicyApplicationChecklistService 전용 — 이 파일이 속한 policy-application 핵심
    // 흐름(register/changeStatus 등)에선 쓰이지 않는다.
    CHECKLIST_NOT_FOUND("P005", "체크리스트 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    // PolicyApplicationService.validateWithinPolicyDeadline()에서 던진다 — register()의 resolveEndAt()과
    // updateEndAt()이 공통으로 이 검증을 거친다(개인 마감일이 정책 자체 마감일보다 늦으면 안 됨).
    END_AT_AFTER_POLICY_DEADLINE("P006", "개인 마감일은 정책 신청 마감일을 넘을 수 없습니다.", HttpStatus.BAD_REQUEST),
    // PolicyApplicationController.parseStatus()에서 던진다 — DTO/@RequestParam의 @Pattern이 이미 걸러주는
    // 값이라 평소엔 발생하지 않지만, ApplicationStatus enum과 두 곳의 @Pattern 문자열이 어긋나는 경우를 대비한
    // 방어선이다(ApplicationStatus.java 클래스 주석 참고). 이게 없으면 valueOf()의 IllegalArgumentException이
    // GlobalExceptionHandler의 Exception.class 핸들러로 떨어져 500(S001)으로 샌다.
    INVALID_APPLICATION_STATUS("P007", "유효하지 않은 상태값입니다.", HttpStatus.BAD_REQUEST),
    // 정책 비교는 DB에 저장하지 않는 stateless 설계다(PolicyComparisonService 클래스 주석 참고). comparisonId는
    // 비교 대상 policyId를 정렬해 이어붙인 문자열일 뿐이라, 형식이 잘못됐거나(파싱 실패) policyId가 2개 미만이면
    // "그런 비교는 애초에 존재할 수 없다"는 의미로 이 코드를 던진다.
    COMPARISON_NOT_FOUND("P008", "존재하지 않는 정책 비교입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
