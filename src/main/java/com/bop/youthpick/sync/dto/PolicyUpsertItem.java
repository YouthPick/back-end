package com.bop.youthpick.sync.dto;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.Region;
import java.util.List;

/**
 * Writer 입력 1건 — 전처리된 정책(비영속) + 적용 지역. 지역을 {@code PolicyRegion}이 아니라 {@code Region}으로 받는 이유: 변경
 * UPDATE 경로에서는 링크를 DB의 관리 엔티티에 다시 걸어야 하므로 링크 생성은 Writer의 몫이다.
 */
public record PolicyUpsertItem(Policy policy, List<Region> regions) {}
