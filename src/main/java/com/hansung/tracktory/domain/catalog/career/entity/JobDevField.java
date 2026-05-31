package com.hansung.tracktory.domain.catalog.career.entity;

import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직무 ↔ 개발 분야 연결 — 직무가 어떤 개발 분야에 속하는지 표현한다. */
@Entity
@Table(
    name = "job_dev_field",
    uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "dev_field_id"}))
@Getter
@NoArgsConstructor
public class JobDevField extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dev_field_id", nullable = false)
  private DevField devField;

  @Builder
  public JobDevField(Job job, DevField devField) {
    this.job = job;
    this.devField = devField;
  }
}
