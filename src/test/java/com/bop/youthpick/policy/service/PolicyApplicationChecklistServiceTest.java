package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PolicyApplicationChecklistServiceTest {

    @Mock private PolicyApplicationChecklistRepository applicationChecklistRepository;
    @Mock private PolicyApplicationRepository policyApplicationRepository;

    private PolicyApplicationChecklistService checklistService;

    private static final Long APPLICATION_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @BeforeEach
    void setUp() {
        checklistService =
                new PolicyApplicationChecklistService(
                        applicationChecklistRepository, policyApplicationRepository);
    }

    /** 소유자가 USER_ID인 활성 신청. owner.getId()는 소유권 검증에 안 걸리는 테스트에선 안 쓰일 수 있어 lenient로 둔다. */
    private PolicyApplication application() {
        User owner = mock(User.class);
        lenient().when(owner.getId()).thenReturn(USER_ID);
        return PolicyApplication.register(
                owner, mock(Policy.class), ApplicationStatus.APPLIED, null, null);
    }

    @Test
    void add_체크리스트를_정상적으로_생성한다() {
        PolicyApplication application = application();
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(applicationChecklistRepository.save(any(PolicyApplicationChecklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicyApplicationChecklist result =
                checklistService.add(APPLICATION_ID, USER_ID, "제출 서류 준비");

        assertThat(result.getContent()).isEqualTo("제출 서류 준비");
        assertThat(result.isChecked()).isFalse();
    }

    @Test
    void add_대상_신청관리가_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.add(APPLICATION_ID, USER_ID, "제출 서류 준비"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void add_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        PolicyApplication application = application();
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> checklistService.add(APPLICATION_ID, OTHER_USER_ID, "제출 서류 준비"))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void check_체크상태로_변경한다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checklistService.check(5L, USER_ID);

        assertThat(checklist.isChecked()).isTrue();
    }

    @Test
    void uncheck_체크해제_상태로_변경한다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        checklist.check();
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checklistService.uncheck(5L, USER_ID);

        assertThat(checklist.isChecked()).isFalse();
    }

    @Test
    void check_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.check(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void uncheck_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.uncheck(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void delete_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.delete(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void delete_soft_delete한다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checklistService.delete(5L, USER_ID);

        assertThat(checklist.getDeletedAt()).isNotNull();
    }

    @Test
    void check_상위_신청관리가_삭제되었으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        PolicyApplication deletedApplication = application();
        deletedApplication.delete();
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(deletedApplication, "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.check(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void uncheck_상위_신청관리가_삭제되었으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        PolicyApplication deletedApplication = application();
        deletedApplication.delete();
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(deletedApplication, "제출 서류 준비");
        checklist.check();
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.uncheck(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void delete_상위_신청관리가_삭제되었으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        PolicyApplication deletedApplication = application();
        deletedApplication.delete();
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(deletedApplication, "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.delete(5L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void check_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.check(5L, OTHER_USER_ID))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void uncheck_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        checklist.check();
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.uncheck(5L, OTHER_USER_ID))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void delete_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checklistService.delete(5L, OTHER_USER_ID))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void getByApplication_신청관리별_체크리스트_목록을_반환한다() {
        PolicyApplication application = application();
        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, "제출 서류 준비");
        Pageable pageable = PageRequest.of(0, 20);
        Page<PolicyApplicationChecklist> page = new PageImpl<>(List.of(checklist), pageable, 1);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(applicationChecklistRepository.findByApplication_IdAndDeletedAtIsNull(
                        APPLICATION_ID, pageable))
                .thenReturn(page);

        Page<PolicyApplicationChecklistResponse> result =
                checklistService.getByApplication(APPLICATION_ID, USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).message()).isEqualTo("제출 서류 준비");
    }

    @Test
    void getByApplication_대상_신청관리가_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                checklistService.getByApplication(
                                        APPLICATION_ID, USER_ID, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void getByApplication_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        PolicyApplication application = application();
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(APPLICATION_ID))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(
                        () ->
                                checklistService.getByApplication(
                                        APPLICATION_ID, OTHER_USER_ID, PageRequest.of(0, 20)))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }
}
