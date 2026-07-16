package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class PolicyApplicationServiceTest {

    @Mock private PolicyApplicationRepository policyApplicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyApplicationChecklistRepository policyApplicationChecklistRepository;

    private PolicyApplicationService policyApplicationService;

    private static final Long USER_ID = 1L;
    private static final Long POLICY_ID = 2L;

    @BeforeEach
    void setUp() {
        policyApplicationService =
                new PolicyApplicationService(
                        policyApplicationRepository,
                        userRepository,
                        policyRepository,
                        policyApplicationChecklistRepository);
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
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "메모", null);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.INTERESTED);
        assertThat(result.getMemo()).isEqualTo("메모");
    }

    @Test
    void 마감일을_지정하지_않으면_정책의_신청_마감일을_기본값으로_사용한다() {
        Policy policy = mock(Policy.class);
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 8, 31));
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicyApplication result =
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "메모", null);

        assertThat(result.getEndAt()).isEqualTo(LocalDateTime.of(2026, 8, 31, 0, 0));
    }

    @Test
    void 마감일을_직접_지정하면_정책_마감일_대신_그_값을_사용한다() {
        Policy policy = mock(Policy.class);
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime explicitEndAt = LocalDateTime.of(2026, 9, 15, 18, 0);
        PolicyApplication result =
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "메모", explicitEndAt);

        assertThat(result.getEndAt()).isEqualTo(explicitEndAt);
    }

    @Test
    void 마감일이_정책_마감일과_같으면_등록을_허용한다() {
        Policy policy = mock(Policy.class);
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 8, 31));
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime sameDayEndAt = LocalDateTime.of(2026, 8, 31, 23, 59);
        PolicyApplication result =
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "메모", sameDayEndAt);

        assertThat(result.getEndAt()).isEqualTo(sameDayEndAt);
    }

    @Test
    void 마감일이_정책_마감일을_넘으면_END_AT_AFTER_POLICY_DEADLINE_예외를_던진다() {
        Policy policy = mock(Policy.class);
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 8, 31));
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));

        LocalDateTime tooLateEndAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        assertThatThrownBy(
                        () ->
                                policyApplicationService.register(
                                        USER_ID,
                                        POLICY_ID,
                                        ApplicationStatus.INTERESTED,
                                        "메모",
                                        tooLateEndAt))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.END_AT_AFTER_POLICY_DEADLINE);
    }

    @Test
    void 메모가_빈_문자열이면_null로_저장한다() {
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(mock(Policy.class)));
        when(policyApplicationRepository.save(any(PolicyApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicyApplication result =
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.INTERESTED, "   ", null);

        assertThat(result.getMemo()).isNull();
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
                                policyApplicationService.register(
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
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.APPLIED, "재등록", null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getMemo()).isEqualTo("재등록");
        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void soft_delete된_행을_재활성화할때_마감일을_지정하지_않으면_정책_마감일을_사용한다() {
        Policy policy = mock(Policy.class);
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 10, 1));
        PolicyApplication existing =
                PolicyApplication.register(
                        mock(User.class), policy, ApplicationStatus.INTERESTED, null, null);
        existing.delete();
        when(policyApplicationRepository.findByUser_IdAndPolicy_Id(USER_ID, POLICY_ID))
                .thenReturn(Optional.of(existing));

        PolicyApplication result =
                policyApplicationService.register(
                        USER_ID, POLICY_ID, ApplicationStatus.APPLIED, "재등록", null);

        assertThat(result.getEndAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    void soft_delete된_행을_재활성화하면_이전_체크리스트를_모두_소프트딜리트한다() {
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

        policyApplicationService.register(
                USER_ID, POLICY_ID, ApplicationStatus.APPLIED, "재등록", null);

        verify(policyApplicationChecklistRepository).softDeleteAllByApplicationId(existing.getId());
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
                                policyApplicationService.register(
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
                                policyApplicationService.register(
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
                                policyApplicationService.register(
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
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result =
                policyApplicationService.changeStatus(10L, USER_ID, ApplicationStatus.APPLIED);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void changeStatus_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                policyApplicationService.changeStatus(
                                        10L, USER_ID, ApplicationStatus.APPLIED))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void changeStatus_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        Long otherUserId = 999L;
        assertThatThrownBy(
                        () ->
                                policyApplicationService.changeStatus(
                                        10L, otherUserId, ApplicationStatus.APPLIED))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void updateMemo_성공() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, "기존 메모", null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result = policyApplicationService.updateMemo(10L, USER_ID, "새 메모");

        assertThat(result.getMemo()).isEqualTo("새 메모");
    }

    @Test
    void updateMemo_빈_문자열이면_null로_비운다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, "기존 메모", null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result = policyApplicationService.updateMemo(10L, USER_ID, "   ");

        assertThat(result.getMemo()).isNull();
    }

    @Test
    void updateMemo_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyApplicationService.updateMemo(10L, USER_ID, "메모"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void updateMemo_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        Long otherUserId = 999L;
        assertThatThrownBy(() -> policyApplicationService.updateMemo(10L, otherUserId, "메모"))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void updateEndAt_성공() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        LocalDateTime newEndAt = LocalDateTime.of(2026, 12, 31, 23, 59);
        PolicyApplication result = policyApplicationService.updateEndAt(10L, USER_ID, newEndAt);

        assertThat(result.getEndAt()).isEqualTo(newEndAt);
    }

    @Test
    void updateEndAt_null이면_마감일을_비운다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner,
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        LocalDateTime.of(2026, 12, 31, 23, 59));
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result = policyApplicationService.updateEndAt(10L, USER_ID, null);

        assertThat(result.getEndAt()).isNull();
    }

    @Test
    void updateEndAt_정책_마감일을_넘으면_END_AT_AFTER_POLICY_DEADLINE_예외를_던진다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        Policy policy = mock(Policy.class);
        when(policy.getApplicationEndDate()).thenReturn(LocalDate.of(2026, 8, 31));
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, policy, ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        LocalDateTime tooLateEndAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        assertThatThrownBy(() -> policyApplicationService.updateEndAt(10L, USER_ID, tooLateEndAt))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.END_AT_AFTER_POLICY_DEADLINE);
        assertThat(existing.getEndAt()).isNull();
    }

    @Test
    void updateEndAt_null로_비울때는_정책_마감일_검증을_건너뛴다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        Policy policy = mock(Policy.class);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner,
                        policy,
                        ApplicationStatus.INTERESTED,
                        null,
                        LocalDateTime.of(2026, 8, 1, 0, 0));
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        PolicyApplication result = policyApplicationService.updateEndAt(10L, USER_ID, null);

        assertThat(result.getEndAt()).isNull();
    }

    @Test
    void updateEndAt_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyApplicationService.updateEndAt(10L, USER_ID, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void updateEndAt_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        Long otherUserId = 999L;
        assertThatThrownBy(() -> policyApplicationService.updateEndAt(10L, otherUserId, null))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void delete_성공() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        policyApplicationService.delete(10L, USER_ID);

        assertThat(existing.isDeleted()).isTrue();
    }

    @Test
    void delete_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyApplicationService.delete(10L, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void delete_소유자가_아니면_FORBIDDEN_예외를_던진다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        PolicyApplication existing =
                PolicyApplication.register(
                        owner, mock(Policy.class), ApplicationStatus.INTERESTED, null, null);
        when(policyApplicationRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(existing));

        Long otherUserId = 999L;
        assertThatThrownBy(() -> policyApplicationService.delete(10L, otherUserId))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.FORBIDDEN);
    }

    @Test
    void getApplications_사용자의_신청관리_목록을_반환한다() {
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
                policyApplicationService.getApplications(USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).policyTitle()).isEqualTo("정책제목");
        assertThat(result.getContent().get(0).status()).isEqualTo(ApplicationStatus.APPLIED);
    }
}
