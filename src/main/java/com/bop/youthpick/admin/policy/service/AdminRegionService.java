package com.bop.youthpick.admin.policy.service;

import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRegionService {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> findAll() {
        return regionRepository.findAll().stream().map(RegionResponse::from).toList();
    }
}
