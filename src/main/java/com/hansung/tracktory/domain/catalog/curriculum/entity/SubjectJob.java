package com.hansung.tracktory.domain.catalog.curriculum.entity;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과목 ↔ 직무 연결 — 과목 상세의 "관련 직무" 를 표현한다. */
@Entity
@Table(
    name = "subject_job",
    uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "job_id"}))
@Getter
@NoArgsConstructor
public class SubjectJob extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @Builder
  public SubjectJob(Subject subject, Job job) {
    this.subject = subject;
    this.job = job;
  }
}
