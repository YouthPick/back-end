package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.ApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
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
class PolicyManagementCheckpointServiceTest {

    @Mock private PolicyApplicationChecklistRepository applicationChecklistRepository;
    @Mock private PolicyApplicationRepository policyApplicationRepository;

    private PolicyManagementCheckpointService checkPointService;

    private static final Long MANAGEMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        checkPointService =
                new PolicyManagementCheckpointService(
                        applicationChecklistRepository, policyApplicationRepository);
    }

    private PolicyApplication management() {
        return PolicyApplication.register(
                mock(User.class), mock(Policy.class), ApplicationStatus.APPLIED, null, null);
    }

    @Test
    void add_체크리스트를_정상적으로_생성한다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(MANAGEMENT_ID))
                .thenReturn(Optional.of(management()));
        when(applicationChecklistRepository.save(any(ApplicationChecklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationChecklist result = checkPointService.add(MANAGEMENT_ID, "제출 서류 준비");

        assertThat(result.getContent()).isEqualTo("제출 서류 준비");
        assertThat(result.isChecked()).isFalse();
    }

    @Test
    void add_대상_신청관리가_없으면_MANAGEMENT_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(MANAGEMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkPointService.add(MANAGEMENT_ID, "제출 서류 준비"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.MANAGEMENT_NOT_FOUND);
    }

    @Test
    void check_체크상태로_변경한다() {
        ApplicationChecklist checklist = ApplicationChecklist.create(management(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checkPointService.check(5L);

        assertThat(checklist.isChecked()).isTrue();
    }

    @Test
    void uncheck_체크해제_상태로_변경한다() {
        ApplicationChecklist checklist = ApplicationChecklist.create(management(), "제출 서류 준비");
        checklist.check();
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checkPointService.uncheck(5L);

        assertThat(checklist.isChecked()).isFalse();
    }

    @Test
    void check_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkPointService.check(5L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void uncheck_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkPointService.uncheck(5L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void delete_대상이_없으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkPointService.delete(5L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void delete_soft_delete한다() {
        ApplicationChecklist checklist = ApplicationChecklist.create(management(), "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        checkPointService.delete(5L);

        assertThat(checklist.getDeletedAt()).isNotNull();
    }

    @Test
    void check_상위_신청관리가_삭제되었으면_CHECKLIST_NOT_FOUND_예외를_던진다() {
        PolicyApplication deletedManagement = management();
        deletedManagement.delete();
        ApplicationChecklist checklist = ApplicationChecklist.create(deletedManagement, "제출 서류 준비");
        when(applicationChecklistRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> checkPointService.check(5L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    @Test
    void getByManagement_신청관리별_체크리스트_목록을_반환한다() {
        ApplicationChecklist checklist = ApplicationChecklist.create(management(), "제출 서류 준비");
        Pageable pageable = PageRequest.of(0, 20);
        Page<ApplicationChecklist> page = new PageImpl<>(List.of(checklist), pageable, 1);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(MANAGEMENT_ID))
                .thenReturn(Optional.of(management()));
        when(applicationChecklistRepository.findByApplication_IdAndDeletedAtIsNull(
                        MANAGEMENT_ID, pageable))
                .thenReturn(page);

        Page<ApplicationChecklistResponse> result =
                checkPointService.getByManagement(MANAGEMENT_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).message()).isEqualTo("제출 서류 준비");
    }

    @Test
    void getByManagement_대상_신청관리가_없으면_MANAGEMENT_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(MANAGEMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                checkPointService.getByManagement(
                                        MANAGEMENT_ID, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.MANAGEMENT_NOT_FOUND);
    }
}
