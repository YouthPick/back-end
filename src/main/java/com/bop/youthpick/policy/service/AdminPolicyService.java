package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.AdminPolicyResponse;
import com.bop.youthpick.policy.dto.AdminPolicyUpdateRequest;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.AdminPolicySpecifications;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public Page<AdminPolicyResponse> search(
            String category,
            String visibilityStatus,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        Page<Policy> page =
                policyRepository.findAll(
                        AdminPolicySpecifications.filter(
                                category, visibilityStatus, startDate, endDate),
                        pageable);
        Map<Long, List<String>> regionCodesByPolicyId =
                regionCodesByPolicyId(page.getContent().stream().map(Policy::getId).toList());
        return page.map(
                policy ->
                        AdminPolicyResponse.from(
                                policy,
                                regionCodesByPolicyId.getOrDefault(policy.getId(), List.of())));
    }

    @Transactional
    public AdminPolicyResponse update(Long policyId, AdminPolicyUpdateRequest request) {
        Policy policy = findPolicy(policyId);
        List<Region> regions = resolveRegions(request.regionCodes());

        policy.updateDetails(
                request.policyName(),
                request.organizationName(),
                request.description(),
                request.largeCategory(),
                request.middleCategory(),
                request.applicationStartDate(),
                request.applicationEndDate(),
                request.applicationUrl());

        policyRegionRepository.deleteByPolicyId(policyId);
        regions.forEach(region -> policyRegionRepository.save(PolicyRegion.create(policy, region)));

        return AdminPolicyResponse.from(policy, request.regionCodes());
    }

    @Transactional
    public AdminPolicyResponse updateVisibility(Long policyId, String visibilityStatus) {
        Policy policy = findPolicy(policyId);
        policy.changeVisibility(PolicyVisibility.valueOf(visibilityStatus));
        return AdminPolicyResponse.from(policy, regionCodesOf(policyId));
    }

    @Transactional
    public AdminPolicyResponse softDelete(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.softDelete();
        return AdminPolicyResponse.from(policy, regionCodesOf(policyId));
    }

    private Policy findPolicy(Long policyId) {
        return policyRepository
                .findById(policyId)
                .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
    }

    private List<Region> resolveRegions(List<String> regionCodes) {
        return regionCodes.stream()
                .map(
                        code ->
                                regionRepository
                                        .findById(code)
                                        .orElseThrow(
                                                () ->
                                                        new CustomException(
                                                                PolicyErrorCode.REGION_NOT_FOUND)))
                .toList();
    }

    private List<String> regionCodesOf(Long policyId) {
        return regionCodesByPolicyId(List.of(policyId)).getOrDefault(policyId, List.of());
    }

    private Map<Long, List<String>> regionCodesByPolicyId(List<Long> policyIds) {
        return policyRegionRepository.findByPolicyIdIn(policyIds).stream()
                .collect(
                        Collectors.groupingBy(
                                pr -> pr.getPolicy().getId(),
                                Collectors.mapping(
                                        pr -> pr.getRegion().getCode(), Collectors.toList())));
    }
}
