package com.bop.youthpick.admin.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.admin.policy.dto.AdminPolicyUpdateRequest;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.time.LocalDate;
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
class AdminPolicyServiceTest {

    @Mock private PolicyRepository policyRepository;

    @Mock private PolicyRegionRepository policyRegionRepository;

    @Mock private RegionRepository regionRepository;

    private AdminPolicyService adminPolicyService;

    private static final Long POLICY_ID = 1L;

    private static final AdminPolicyUpdateRequest UPDATE_REQUEST =
            new AdminPolicyUpdateRequest(
                    "정책명",
                    "주관기관",
                    "설명",
                    "일자리",
                    "중분류",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    "https://example.com",
                    List.of("11110"));

    @BeforeEach
    void setUp() {

        adminPolicyService =
                new AdminPolicyService(policyRepository, policyRegionRepository, regionRepository);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_지역코드가_채워진_페이지를_반환한다() {
        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(POLICY_ID);
        Page<Policy> page = new PageImpl<>(List.of(policy));
        when(policyRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        PolicyRegion policyRegion = mock(PolicyRegion.class);
        Region region = mock(Region.class);
        when(policyRegion.getPolicy()).thenReturn(policy);
        when(policyRegion.getRegion()).thenReturn(region);
        when(region.getCode()).thenReturn("11110");
        when(policyRegionRepository.findByPolicyIdIn(List.of(POLICY_ID)))
                .thenReturn(List.of(policyRegion));

        Page<?> result =
                adminPolicyService.search("일자리", "VISIBLE", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 정책_수정_시_지역을_전량_교체한다() {
        Policy policy = mock(Policy.class);
        Region region = mock(Region.class);
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(regionRepository.findById("11110")).thenReturn(Optional.of(region));

        adminPolicyService.update(POLICY_ID, UPDATE_REQUEST);

        verify(policy)
                .updateDetails(
                        "정책명",
                        "주관기관",
                        "설명",
                        "일자리",
                        "중분류",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        "https://example.com");
        verify(policyRegionRepository).deleteByPolicyId(POLICY_ID);
        verify(policyRegionRepository).save(any(PolicyRegion.class));
    }

    @Test
    void 수정_대상_정책이_없으면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPolicyService.update(POLICY_ID, UPDATE_REQUEST))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_지역코드로_수정하면_REGION_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(mock(Policy.class)));
        when(regionRepository.findById("11110")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPolicyService.update(POLICY_ID, UPDATE_REQUEST))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void 노출상태를_변경한다() {
        Policy policy = mock(Policy.class);
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(eq(List.of(POLICY_ID)))).thenReturn(List.of());

        adminPolicyService.updateVisibility(POLICY_ID, "HIDDEN");

        verify(policy).changeVisibility(PolicyVisibility.HIDDEN);
    }

    @Test
    void 노출상태_변경_대상이_없으면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPolicyService.updateVisibility(POLICY_ID, "HIDDEN"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void 정책을_soft_delete_한다() {
        Policy policy = mock(Policy.class);
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(eq(List.of(POLICY_ID)))).thenReturn(List.of());

        adminPolicyService.softDelete(POLICY_ID);

        verify(policy).softDelete();
    }

    @Test
    void 삭제_대상이_없으면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPolicyService.softDelete(POLICY_ID))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.POLICY_NOT_FOUND);
    }
}
