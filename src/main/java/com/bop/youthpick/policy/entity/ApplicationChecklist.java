package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 신청관리별 준비 체크리스트 (제출서류 등). */
@Entity
@Table(
    name = "application_checklists",
    indexes = @Index(name = "idx_application_checklists_app", columnList = "application_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationChecklist extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false)
  private PolicyApplication application;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "is_checked", nullable = false)
  private boolean checked;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
