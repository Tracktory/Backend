package com.hansung.tracktory.domain.catalog.career.entity;

import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직무 ↔ 트랙 연결 — 직무 상세의 "관련 트랙" 을 표현한다. */
@Entity
@Table(
    name = "job_track",
    uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "track_id"}))
@Getter
@NoArgsConstructor
public class JobTrack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @Builder
  public JobTrack(Job job, Track track) {
    this.job = job;
    this.track = track;
  }
}
