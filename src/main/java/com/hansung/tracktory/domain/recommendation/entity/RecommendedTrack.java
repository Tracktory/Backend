package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추천된 트랙 단건 — primary(주 추천) 와 보조 추천을 is_primary 로 구분한다. */
@Entity
@Table(
    name = "recommended_track",
    uniqueConstraints = @UniqueConstraint(columnNames = {"recommendation_id", "track_id"}))
@Getter
@NoArgsConstructor
public class RecommendedTrack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column private Integer score;

  @Column(columnDefinition = "text")
  private String reasoning;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  /** 학과 경계를 넘는 이색 조합 슬롯에서 나온 트랙인지 — AI 의 cross_college 슬롯 분류를 보존한다. */
  @Column(name = "is_cross_combination", nullable = false)
  private boolean crossCombination;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_id", nullable = false)
  private Recommendation recommendation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @Builder
  public RecommendedTrack(
      Integer score, String reasoning, boolean primary, boolean crossCombination, Track track) {
    this.score = score;
    this.reasoning = reasoning;
    this.primary = primary;
    this.crossCombination = crossCombination;
    this.track = track;
  }

  void assignTo(Recommendation owner) {
    this.recommendation = owner;
  }
}
