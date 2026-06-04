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
 * 잔여(추천) 과목 한 건의 충족도 기여 — coverage 분석의 자식. {@code contributionPercent} 는 그 과목을 단독으로 이수했을 때의 독립 한계
 * 증가분(0~100). 과목 간 토큰이 겹칠 수 있어 기여도 합은 누적 도달과 다르므로 "이 과목 가치(+X%)" 배지로만 쓴다.
 */
@Entity
@Table(name = "recommendation_course_contribution")
@Getter
@NoArgsConstructor
public class RecommendationCourseContribution extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coverage_id", nullable = false)
  private RecommendationCoverage coverage;

  @Column(name = "course_code", nullable = false)
  private String courseCode;

  @Column(name = "course_name", nullable = false)
  private String courseName;

  @Column(name = "contribution_percent", nullable = false)
  private int contributionPercent;

  @Builder
  public RecommendationCourseContribution(
      String courseCode, String courseName, int contributionPercent) {
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.contributionPercent = contributionPercent;
  }

  void assignTo(RecommendationCoverage owner) {
    this.coverage = owner;
  }
}
