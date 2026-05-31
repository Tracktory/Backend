package com.hansung.tracktory.domain.catalog.career.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직무 ↔ 기술 스택 연결 — 직무가 요구하는 기술과 그 학습 단계를 표현한다. */
@Entity
@Table(
    name = "job_tech_stack",
    uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "tech_stack_id"}))
@Getter
@NoArgsConstructor
public class JobTechStack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JobTechStackStage stage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tech_stack_id", nullable = false)
  private TechStack techStack;

  @Builder
  public JobTechStack(JobTechStackStage stage, Job job, TechStack techStack) {
    this.stage = stage;
    this.job = job;
    this.techStack = techStack;
  }
}
