package com.hansung.tracktory.domain.recommendation.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분야(추천 직무)별 역량 충족도 — coverage 분석의 자식. 비율은 저장하지 않고 {@code coveredCount / requiredCount} 로 조회 시점에
 * 파생한다(현재 ≤ 예상 불변을 카운트가 보장). {@code missingTokens} 는 예상 이수 후에도 못 덮는 토큰의 표시용 문자열(개행 결합).
 */
@Entity
@Table(name = "recommendation_job_coverage")
@Getter
@NoArgsConstructor
public class RecommendationJobCoverage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coverage_id", nullable = false)
  private RecommendationCoverage coverage;

  @Column(name = "job_code", nullable = false)
  private String jobCode;

  @Column(name = "job_name", nullable = false)
  private String jobName;

  @Column(name = "required_count", nullable = false)
  private int requiredCount;

  @Column(name = "current_covered", nullable = false)
  private int currentCovered;

  @Column(name = "expected_covered", nullable = false)
  private int expectedCovered;

  @Column(name = "missing_tokens", columnDefinition = "text")
  private String missingTokens;

  @Builder
  public RecommendationJobCoverage(
      String jobCode,
      String jobName,
      int requiredCount,
      int currentCovered,
      int expectedCovered,
      String missingTokens) {
    this.jobCode = jobCode;
    this.jobName = jobName;
    this.requiredCount = requiredCount;
    this.currentCovered = currentCovered;
    this.expectedCovered = expectedCovered;
    this.missingTokens = missingTokens;
  }

  void assignTo(RecommendationCoverage owner) {
    this.coverage = owner;
  }
}
