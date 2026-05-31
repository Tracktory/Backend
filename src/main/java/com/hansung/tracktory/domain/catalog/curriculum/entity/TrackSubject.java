package com.hansung.tracktory.domain.catalog.curriculum.entity;

import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 트랙 ↔ 과목 연결 — 트랙 내 과목의 졸업요건 유형과 학습 단계를 표현한다. */
@Entity
@Table(
    name = "track_subject",
    uniqueConstraints = @UniqueConstraint(columnNames = {"track_id", "subject_id"}))
@Getter
@NoArgsConstructor
public class TrackSubject extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectStage stage;

  @Builder
  public TrackSubject(Track track, Subject subject, SubjectType type, SubjectStage stage) {
    this.track = track;
    this.subject = subject;
    this.type = type;
    this.stage = stage;
  }
}
