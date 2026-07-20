package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RegionRepository extends JpaRepository<Region, String> {

    /** 시도 개수 — 정책이 전 시도를 커버하면 지역 라벨을 '전국'으로 접는 판정에 쓴다. */
    @Query("select count(distinct r.sidoName) from Region r")
    long countDistinctSidoNames();
}
