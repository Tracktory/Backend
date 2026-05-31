package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
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

/** 학기 그룹 내 추천 과목 항목 — score 는 과목 단위 점수(알고리즘 미확정, nullable). */
@Entity
@Table(
    name = "roadmap_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"roadmap_semester_id", "subject_id"}))
@Getter
@NoArgsConstructor
public class RoadmapItem extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_semester_id", nullable = false)
  private RoadmapSemester roadmapSemester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @Column private Integer score;

  @Builder
  public RoadmapItem(Subject subject, Integer score) {
    this.subject = subject;
    this.score = score;
  }

  void assignTo(RoadmapSemester owner) {
    this.roadmapSemester = owner;
  }
}
