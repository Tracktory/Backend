package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추천 트랜잭션 aggregate root — 직무·트랙·로드맵 추천을 한 단위로 묶고 이력 상태를 관리한다. */
@Entity
@Table(name = "recommendation")
@Getter
@NoArgsConstructor
public class Recommendation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RecommendationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_source", nullable = false)
  private RecommendationTriggerSource triggerSource;

  @Column(name = "track_combination_score")
  private Integer trackCombinationScore;

  @Column(name = "track_combination_summary")
  private String trackCombinationSummary;

  @Column(name = "track_combination_reasoning", columnDefinition = "text")
  private String trackCombinationReasoning;

  @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendedJob> recommendedJobs = new ArrayList<>();

  @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendedTrack> recommendedTracks = new ArrayList<>();

  @OneToOne(
      mappedBy = "recommendation",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Roadmap roadmap;

  @Builder
  public Recommendation(
      User user,
      RecommendationStatus status,
      RecommendationTriggerSource triggerSource,
      Integer trackCombinationScore,
      String trackCombinationSummary,
      String trackCombinationReasoning) {
    this.user = user;
    this.status = status;
    this.triggerSource = triggerSource;
    this.trackCombinationScore = trackCombinationScore;
    this.trackCombinationSummary = trackCombinationSummary;
    this.trackCombinationReasoning = trackCombinationReasoning;
  }

  /** 추천 직무 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addRecommendedJob(RecommendedJob job) {
    recommendedJobs.add(job);
    job.assignTo(this);
  }

  /** 추천 트랙 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addRecommendedTrack(RecommendedTrack track) {
    recommendedTracks.add(track);
    track.assignTo(this);
  }

  /** 로드맵 1:1 자식을 연결하고 양방향 연관을 맞춘다. */
  public void attachRoadmap(Roadmap newRoadmap) {
    this.roadmap = newRoadmap;
    newRoadmap.assignTo(this);
  }

  /** 새 추천으로 대체되었음을 표시한다 (메인 노출 제외, 이력으로만 조회). */
  public void markSuperseded() {
    this.status = RecommendationStatus.SUPERSEDED;
  }
}
