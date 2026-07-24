package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, String> {

    /** 시도명 → 시군구명 순 한글 가나다 정렬. 온보딩/관리자 지역 선택지가 이 순서를 그대로 쓴다. */
    List<Region> findAllByOrderBySidoNameAscNameAsc();
}
