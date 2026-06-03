package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추천된 직무 단건 — recommendation aggregate 의 자식. score 는 0~100 적합도. */
@Entity
@Table(
    name = "recommended_job",
    uniqueConstraints = @UniqueConstraint(columnNames = {"recommendation_id", "job_id"}))
@Getter
@NoArgsConstructor
public class RecommendedJob extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer score;

  @Column(columnDefinition = "text")
  private String reasoning;

  @ElementCollection
  @CollectionTable(
      name = "recommended_job_competency_tag",
      joinColumns = @JoinColumn(name = "recommended_job_id"))
  @OrderColumn(name = "tag_order")
  @Column(name = "tag", nullable = false)
  private List<String> competencyTags = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_id", nullable = false)
  private Recommendation recommendation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @Builder
  public RecommendedJob(Integer score, String reasoning, List<String> competencyTags, Job job) {
    this.score = score;
    this.reasoning = reasoning;
    this.competencyTags =
        competencyTags == null ? new ArrayList<>() : new ArrayList<>(competencyTags);
    this.job = job;
  }

  void assignTo(Recommendation owner) {
    this.recommendation = owner;
  }
}
