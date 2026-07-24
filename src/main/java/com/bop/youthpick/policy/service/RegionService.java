package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> getAllRegions() {
        return regionRepository.findAllByOrderBySidoNameAscNameAsc().stream()
                .map(RegionResponse::from)
                .toList();
    }
}
