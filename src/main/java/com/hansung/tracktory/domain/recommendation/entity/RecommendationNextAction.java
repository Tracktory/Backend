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
 * 추천 기반 다음 액션 한 건 — coverage 분석의 자식. 잔여 과목 중 충족도를 가장 크게 끌어올리는 과목 제안. {@code orderIndex} 로 AI 가 매긴
 * 기여도 순서를 보존한다. {@code contributionPercent} 는 이 과목 단독 이수 시 증가분(0~100, 독립값).
 */
@Entity
@Table(name = "recommendation_next_action")
@Getter
@NoArgsConstructor
public class RecommendationNextAction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coverage_id", nullable = false)
  private RecommendationCoverage coverage;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "course_code", nullable = false)
  private String courseCode;

  @Column(name = "course_name", nullable = false)
  private String courseName;

  @Column(name = "contribution_percent", nullable = false)
  private int contributionPercent;

  @Column(columnDefinition = "text")
  private String message;

  @Builder
  public RecommendationNextAction(
      int orderIndex,
      String courseCode,
      String courseName,
      int contributionPercent,
      String message) {
    this.orderIndex = orderIndex;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.contributionPercent = contributionPercent;
    this.message = message;
  }

  void assignTo(RecommendationCoverage owner) {
    this.coverage = owner;
  }
}
