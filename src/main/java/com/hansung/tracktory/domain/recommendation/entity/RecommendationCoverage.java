package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 직무 요구 역량 대비 현재 → 예상 충족도 분석 — recommendation 과 1:1. AI 추천 응답이 추천 생성 시점에 산출한 스냅샷을 그대로 보존해, 상세 분석
 * 리포트가 홈 추천과 같은 기준으로 조회되도록 한다.
 *
 * <p>비율은 저장하지 않고 카운트({@code coveredCount / requiredCount})로 조회 시점에 파생한다. 세 카운트가 {@code
 * currentCovered ≤ nextActionsCovered ≤ expectedCovered} 를 만족하므로 화면의 3단(현재 → 다음 N개 → 전체) 표기 불변이
 * 보장된다. {@code nextActionsCovered} 는 다음 액션 shortlist 까지 이수 시 덮는 토큰의 합집합 수로, AI 가 계산해 내려준 값을 그대로
 * 보존한다(중복 토큰 이중 계산 없음).
 */
@Entity
@Table(name = "recommendation_coverage")
@Getter
@NoArgsConstructor
public class RecommendationCoverage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
  private Recommendation recommendation;

  @Column(name = "required_count", nullable = false)
  private int requiredCount;

  @Column(name = "current_covered", nullable = false)
  private int currentCovered;

  @Column(name = "next_actions_covered", nullable = false)
  private int nextActionsCovered;

  @Column(name = "expected_covered", nullable = false)
  private int expectedCovered;

  @Column(name = "gap_tokens", columnDefinition = "text")
  private String gapTokens;

  @OneToMany(mappedBy = "coverage", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationJobCoverage> jobCoverages = new ArrayList<>();

  @OneToMany(mappedBy = "coverage", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationCourseContribution> courseContributions = new ArrayList<>();

  @OneToMany(mappedBy = "coverage", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationNextAction> nextActions = new ArrayList<>();

  @Builder
  public RecommendationCoverage(
      int requiredCount,
      int currentCovered,
      int nextActionsCovered,
      int expectedCovered,
      String gapTokens) {
    this.requiredCount = requiredCount;
    this.currentCovered = currentCovered;
    this.nextActionsCovered = nextActionsCovered;
    this.expectedCovered = expectedCovered;
    this.gapTokens = gapTokens;
  }

  void assignTo(Recommendation owner) {
    this.recommendation = owner;
  }

  /** 분야(직무)별 충족도 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addJobCoverage(RecommendationJobCoverage jobCoverage) {
    jobCoverages.add(jobCoverage);
    jobCoverage.assignTo(this);
  }

  /** 과목별 기여 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addCourseContribution(RecommendationCourseContribution contribution) {
    courseContributions.add(contribution);
    contribution.assignTo(this);
  }

  /** 다음 액션 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addNextAction(RecommendationNextAction nextAction) {
    nextActions.add(nextAction);
    nextAction.assignTo(this);
  }
}
