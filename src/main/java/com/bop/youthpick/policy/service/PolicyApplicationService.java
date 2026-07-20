package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service // 서비스 계층 등록
@RequiredArgsConstructor // final 필드에 대한 생성자를 별도로 생성하지 않고 Lombok이 자동으로 생성자를 생성해준다.
public class PolicyApplicationService {

    // 사용자 정보,정책 신청 관리, 정책, 정책 신청 체크리스트의 객체를 참조한다.
    private final PolicyApplicationRepository policyApplicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyApplicationChecklistRepository policyApplicationChecklistRepository;

    // 여러 DB 변경 작업을 하나로 묶어서 작업하는 에노테이션을 트랜잭션이라고 한다. 사용자가 정책 등록같은 행동을 취했을 때 작동하고 하나라도 오류 날 시 롤백해준다.
    // register 메서드는 컨트롤러로부터 사용자 ID, 정책 ID, 상태(준비중같은 상태 말하는거임), 메모, 마감일을 매개변수로 전달받는다.
    // existing은 policyApplicationRepository에게 사용자 id와 정책 id로 조회를 요청한 결과를 할당한다.
    // 기존 정책 신청 내역 조회 결과가 없으면 null로 처리한다.
    // 메모가 비어있거나 공백이면 null로 통일하고, 값이 있으면 원본 그대로 사용한다(내용 가공 없음).
    @Transactional
    public PolicyApplication register(
            Long userId,
            Long policyId,
            ApplicationStatus status,
            String memo,
            LocalDateTime endAt) {
        PolicyApplication existing =
                policyApplicationRepository
                        .findIncludingDeletedByUserIdAndPolicyId(userId, policyId)
                        .orElse(null);
        String normalizedMemo = blankToNull(memo);

        // 이 조건문은 기존 신청 내역이 존재하는 경우 이 조건문이 실행된다.
        // 기존 신청 내역이 삭제되지 않았다면 예외를 발생시켜 이미 존재하는 정책이라고 반환한다.
        // 만약에 삭제 내역이 있다면 reactivate를 통해 다시 활성화한다.(상태, 메모, 마감일등을 갱신한다. 기존 신청에 연결된 체크리스트(id)를
        // 소프트삭제한다.)
        // 다시 활성화된 신청 정보를 반환한다.
        if (existing != null) {
            if (!existing.isDeleted()) {
                throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
            }
            existing.reactivate(status, normalizedMemo, resolveEndAt(endAt, existing.getPolicy()));
            policyApplicationChecklistRepository.softDeleteAllByApplicationId(existing.getId());
            return existing;
        }
        // user는 사용자 리포의 사용자 id를 조회한다.
        // 사용자가 존재하지 않을 시 예외를 발생시켜 사용자를 찾을 수 없다고 반환한다.
        // 조회된 사용자는 user에 할당한다.
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        // policy는 정책 리포의 정책 id를 조회한다.
        // 정책 조회가 되지 않는다면 예외를 발생시켜 정책을 찾을 수 없다고 반환한다.
        // 정책 조회가 된다면 policy에 할당한다.
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
        // PolicyApplication.register 정적 팩토리 메서드에 user, policy, status, memo, endAt을 인자로 넘겨 새
        // PolicyApplication 객체를 만든다.
        // 생성된 객체를 application 변수에 할당한다.
        PolicyApplication application =
                PolicyApplication.register(
                        user, policy, status, normalizedMemo, resolveEndAt(endAt, policy));

        // 리포 save 메서드에 application 정보를 저장한다.
        // 저장된 정책 신청 객체를 반환한다.
        try {
            return policyApplicationRepository.save(application);

            // 동시에 같은 정책 신청 요청이 들어와 유니크 제약조건 위반이 발생한 경우
            // 중복 신청 예외로 변환한다.
        } catch (DataIntegrityViolationException e) {
            // including-deleted 조회 확인 이후 동시 요청이 먼저 저장한 경우
            // (uk_policy_applications_user_policy UNIQUE 위반). 같은 도메인 에러로 통일한다.
            throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
        }
    }

    // 여러 DB 작업들을 하나로 묶어서 작업하는 걸 트랜잭션이라고 한다 오류가 하나라도 발생하면 롤백한다.
    // changeStatus 메서드는 컨트롤러로부터 PK id, 사용자 id, 상태를 매개변수로 전달받는다.
    // application은 findActive(id)로 삭제되지 않은 신청 내역을 조회한 결과를 할당한다.
    // application.verifyOwner(userId)로 해당 정책 신청이 요청한 사용자의 것이 맞는지 확인한다.
    // application.changeStatus(status)로 상태를 변경한다(트랜잭션 커밋 시 자동으로 DB에 반영되고, 별도 save 호출은 필요 없다). 변경된
    // application을 반환한다.
    @Transactional
    public PolicyApplication changeStatus(Long id, Long userId, ApplicationStatus status) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.changeStatus(status);
        return application;
    }

    // 여러 DB 작업들을 하나로 묶어서 작업하는 걸 트랜잭션이라고한다. 하나라도 오류 날 시 변경 값들을 롤백한다.
    // updateMemo 메서드는 컨트롤러로부터 PK id, 사용자 id, 메모를 매개변수로 전달받는다.
    // application은 findActive(id)로 삭제되지 않은 신청 내역을 조회하고, application.verifyOwner(userId)로 요청자가 소유자인지
    // 확인한다.

    @Transactional
    public PolicyApplication updateMemo(Long id, Long userId, String memo) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.updateMemo(blankToNull(memo));
        return application;
    }

    // 여러 DB 변경 작업들을 하나로 묶어서 처리하는 걸 트랜잭션이라고 한다. 하나라도 오류 날 시 변경값을 롤백한다.
    // updateEndAt 메서드는 컨트롤러로부터 pk, 사용자 id, 마감일(endAt)을 매개변수로 전달받는다.
    // application은 findActive(id)로 삭제되지 않은 신청 내역을 조회한 결과를 할당한다.
    // application.verifyOwner(userId)로 해당 신청이 요청한 사용자의 것이 맞는지 확인한다.
    // 마감일이 null이 아니면 정책 자체의 신청 마감일을 넘지 않는지 검증한다.
    // application.updateEndAt(endAt)으로 마감일을 갱신하고, 변경된 application을 반환한다.
    @Transactional
    public PolicyApplication updateEndAt(Long id, Long userId, LocalDateTime endAt) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        if (endAt != null) {
            validateWithinPolicyDeadline(endAt, application.getPolicy());
        }
        application.updateEndAt(endAt);
        return application;
    }

    // 여러 DB 변경 작업들을 하나로 묶어서 작업하는 에노테이션을 트랜잭션이라고한다. 리드온리는 오직 읽기만 가능하다.
    // getApplications 메서드는 컨트롤러로부터 사용자 id와 Pageable을 매개변수로 전달받는다.
    // policyApplicationRepository에게 해당 사용자의 삭제되지 않은 정책 신청 목록을 조회한다.
    // 조회 결과를 PolicyApplicationResponse DTO로 변환하여 Page 형태로 반환한다.
    @Transactional(readOnly = true)
    public Page<PolicyApplicationResponse> getApplications(Long userId, Pageable pageable) {
        return policyApplicationRepository
                .findByUser_IdAndDeletedAtIsNull(userId, pageable)
                .map(PolicyApplicationResponse::from);
    }

    // 여러 DB 작업들을 하나로 묶어서 작업하는 트랜잭션
    // 사용자가 삭제 할 때 PK ID, 사용자 ID등을 매개변수를 통해 전달받는다.
    // application은 pk id가 삭제되지 않았는지 조회한다.
    // 해당 삭제를 사용자가 한게 맞는지 사용자 ID를 확인한다.
    // 다른 사람 신청이면 권한 예외를 발생시킨다.
    // 정책 신청을 삭제 처리한다.
    @Transactional
    public void delete(Long id, Long userId) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.delete();
    }

    // 해당 ID가 지워지지 않았는지 확인한다.
    // 리포에게 PK ID를 확인하여 존재하지 않으면 정책 신청이 존재하지 않는다고 에러를 퍼트린다.
    private PolicyApplication findActive(Long id) {
        return policyApplicationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
    }

    // 메모가 비어있거나 공백이면 null로 통일하고, 값이 있으면 원본 그대로(가공 없이) 반환하는 메서드
    private static String blankToNull(String memo) {
        return StringUtils.hasText(memo) ? memo : null;
    }

    /** 개인이 마감일을 지정하지 않으면(null) 정책 자체의 신청 마감일을 기본값으로 잡는다. 직접 지정한 경우엔 정책 마감일을 넘지 않는지 검증한다. */
    // 정책 마감일을 확인하는 코드
    private static LocalDateTime resolveEndAt(LocalDateTime requestedEndAt, Policy policy) {
        if (requestedEndAt == null) {
            return policy.getApplicationEndDate() != null
                    ? policy.getApplicationEndDate().atStartOfDay()
                    : null;
        }
        validateWithinPolicyDeadline(requestedEndAt, policy);
        return requestedEndAt;
    }

    /** 정책 자체의 신청 마감일이 알려져 있다면(null이 아니면), 개인 마감일이 그 날짜를 넘지 않아야 한다. */
    private static void validateWithinPolicyDeadline(LocalDateTime endAt, Policy policy) {
        if (policy.getApplicationEndDate() != null
                && endAt.toLocalDate().isAfter(policy.getApplicationEndDate())) {
            throw new CustomException(PolicyErrorCode.END_AT_AFTER_POLICY_DEADLINE);
        }
    }
}
