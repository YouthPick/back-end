package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PolicyManagementServiceTest {

    @Mock private PolicyApplicationRepository policyApplicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private PolicyRepository policyRepository;

    private PolicyManagementService policyManagementService;

    private static final Long USER_ID = 1L;
    private static final Long POLICY_ID = 2L;

    @BeforeEach
    void setUp() {
        policyManagementService =
                new PolicyManagementService(
                        policyApplicationRepository, userRepository, policyRepository);
    }

    @Test
    void 신규_신청관리를_정상적으로_등록한다() {
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(mock(Policy.class)));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicyApplication result =
                policyManagementService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "메모", null);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.INTERESTED);
        assertThat(result.getMemo()).isEqualTo("메모");
    }

    @Test
    void 이미_등록된_행이_있으면_POLICY_ALREADY_EXISTS_예외를_던진다() {
        PolicyApplication existing =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        null);
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                policyManagementService.register(
                                        USER_ID, POLICY_ID, ApplicationStatus.APPLIED, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_ALREADY_EXISTS);
    }

    @Test
    void soft_delete된_행이_있으면_재활성화한다() {
        PolicyApplication existing =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        null);
        existing.delete();
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.of(existing));

        PolicyApplication result =
                policyManagementService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.APPLIED, "재등록", null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getMemo()).isEqualTo("재등록");
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void 저장_시점에_동시요청으로_유니크_제약이_위반되면_POLICY_ALREADY_EXISTS_예외를_던진다() {
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(mock(Policy.class)));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenThrow(
                        new DataIntegrityViolationException("uk_policy_applications_user_policy"));

        assertThatThrownBy(
                        () ->
                                policyManagementService.register(
                                        USER_ID,
                                        POLICY_ID,
                                        ApplicationStatus.INTERESTED,
                                        null,
                                        null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_ALREADY_EXISTS);
    }

    @Test
    void 존재하지_않는_사용자면_USER_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                policyManagementService.register(
                                        USER_ID,
                                        POLICY_ID,
                                        ApplicationStatus.INTERESTED,
                                        null,
                                        null))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_정책이면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                policyManagementService.register(
                                        USER_ID,
                                        POLICY_ID,
                                        ApplicationStatus.INTERESTED,
                                        null,
                                        null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void changeStatus_성공() {
        PolicyApplication existing =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result =
                policyManagementService.changeStatus(10L, ApplicationStatus.APPLIED);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void changeStatus_대상이_없으면_MANAGEMENT_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> policyManagementService.changeStatus(10L, ApplicationStatus.APPLIED))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.MANAGEMENT_NOT_FOUND);
    }

    @Test
    void delete_성공() {
        PolicyApplication existing =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        policyManagementService.delete(10L);

        assertThat(existing.isDeleted()).isTrue();
    }

    @Test
    void getManagements_사용자의_신청관리_목록을_반환한다() {
        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(POLICY_ID);
        when(policy.getTitle()).thenReturn("정책제목");
        when(policy.getCategory()).thenReturn("일자리");
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class), policy, ApplicationStatus.APPLIED, "메모", null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<PolicyApplication> page = new PageImpl<>(List.of(application), pageable, 1);
        when(policyApplicationRepository.findByUser_IdAndDeletedAtIsNull(USER_ID, pageable))
                .thenReturn(page);

        Page<PolicyApplicationResponse> result =
                policyManagementService.getManagements(USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).policyTitle()).isEqualTo("정책제목");
        assertThat(result.getContent().get(0).status()).isEqualTo(ApplicationStatus.APPLIED);
    }
}
