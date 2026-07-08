package com.bop.youthpick.policy.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 정책 적용지역 (zipCd 콤마목록 → 행 단위 정규화). 배치가 통째로 갈아끼우는 데이터라 타임스탬프/soft delete 없음. */
@Entity
@Table(
    name = "policy_regions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_policy_regions",
            columnNames = {"policy_id", "region_code"}),
    indexes = @Index(name = "idx_policy_regions_region", columnList = "region_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyRegion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "policy_id", nullable = false)
  private Policy policy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_code", nullable = false)
  private Region region;
}
