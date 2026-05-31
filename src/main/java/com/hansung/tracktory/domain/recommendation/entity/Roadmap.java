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

/** 추천 로드맵 — recommendation 과 1:1, 학기 그룹(roadmap_semester) 을 자식으로 갖는다. */
@Entity
@Table(name = "roadmap")
@Getter
@NoArgsConstructor
public class Roadmap extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
  private Recommendation recommendation;

  @Column(columnDefinition = "text")
  private String reasoning;

  @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RoadmapSemester> semesters = new ArrayList<>();

  @Builder
  public Roadmap(String reasoning) {
    this.reasoning = reasoning;
  }

  void assignTo(Recommendation owner) {
    this.recommendation = owner;
  }

  /** 학기 그룹 자식을 추가하고 양방향 연관을 맞춘다. */
  public void addSemester(RoadmapSemester semester) {
    semesters.add(semester);
    semester.assignTo(this);
  }
}
