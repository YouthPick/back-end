package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyChatAccessServiceTest {

    @Mock private PolicyRepository policyRepository;
    @InjectMocks private PolicyChatAccessService accessService;

    @Test
    void 숨김_삭제_미존재_정책은_모두_POLICY_NOT_FOUND로_거부한다() {
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                        10L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireVisiblePolicy(10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
    }
}
