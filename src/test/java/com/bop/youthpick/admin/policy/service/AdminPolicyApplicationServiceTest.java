package com.bop.youthpick.admin.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminPolicyApplicationServiceTest {

    @Mock private PolicyApplicationRepository policyApplicationRepository;

    @Mock private PolicyApplicationChecklistRepository applicationChecklistRepository;

    private AdminPolicyApplicationService adminPolicyApplicationService;

    private static final Long APPLICATION_ID = 1L;

    @BeforeEach
    void setUp() {
        adminPolicyApplicationService =
                new AdminPolicyApplicationService(
                        policyApplicationRepository, applicationChecklistRepository);
    }

    private PolicyApplication mockApplication() {
        PolicyApplication application = mock(PolicyApplication.class);
        User user = mock(User.class);
        Policy policy = mock(Policy.class);
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(10L);
        when(application.getPolicy()).thenReturn(policy);
        when(policy.getId()).thenReturn(20L);
        when(policy.getTitle()).thenReturn("정책명");
        return application;
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        Page<PolicyApplication> page = new PageImpl<>(List.of(mockApplication()));
        when(policyApplicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result =
                adminPolicyApplicationService.search(
                        10L, "정책", "INTERESTED", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 체크리스트를_조회한다() {
        when(policyApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(mock(PolicyApplication.class)));
        PolicyApplicationChecklist item = mock(PolicyApplicationChecklist.class);
        PolicyApplication application = mock(PolicyApplication.class);
        when(item.getId()).thenReturn(100L);
        when(item.getApplication()).thenReturn(application);
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(item.isChecked()).thenReturn(true);
        when(item.getContent()).thenReturn("주민등록등본 제출");
        when(applicationChecklistRepository.findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(
                        eq(APPLICATION_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        List<?> result = adminPolicyApplicationService.getChecklist(APPLICATION_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void 체크리스트_조회_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPolicyApplicationService.getChecklist(APPLICATION_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void 상태를_변경한다() {
        PolicyApplication application = mockApplication();
        when(policyApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(application));
        when(application.getStatus()).thenReturn(ApplicationStatus.APPLIED);

        var result = adminPolicyApplicationService.updateStatus(APPLICATION_ID, "APPLIED");

        verify(application).changeStatus(ApplicationStatus.APPLIED);
        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void 상태_변경_대상이_없으면_POLICY_APPLICATION_NOT_FOUND_예외를_던진다() {
        when(policyApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> adminPolicyApplicationService.updateStatus(APPLICATION_ID, "APPLIED"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND);
    }

    @Test
    void 유효하지_않은_상태값이면_INVALID_APPLICATION_STATUS_예외를_던진다() {
        when(policyApplicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(mock(PolicyApplication.class)));

        assertThatThrownBy(
                        () ->
                                adminPolicyApplicationService.updateStatus(
                                        APPLICATION_ID, "INVALID_STATUS"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.INVALID_APPLICATION_STATUS);
    }
}
