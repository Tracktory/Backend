package com.hansung.tracktory.domain.recommendation.entity;

import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추천 로드맵의 학기 그룹 — 학년·학기와 학습 단계 라벨을 갖는다. 컬럼명 grade_year 는 예약어 충돌을 피하기 위함이며 의미는 대학 학년(1~4)이다. */
@Entity
@Table(
    name = "roadmap_semester",
    uniqueConstraints = @UniqueConstraint(columnNames = {"roadmap_id", "grade_year", "semester"}))
@Getter
@NoArgsConstructor
public class RoadmapSemester extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", nullable = false)
  private Roadmap roadmap;

  @Column(name = "grade_year", nullable = false)
  private int year;

  @Column(name = "semester", nullable = false)
  private int semester;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectStage stage;

  @Column(columnDefinition = "text")
  private String reasoning;

  @OneToMany(mappedBy = "roadmapSemester", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RoadmapItem> items = new ArrayList<>();

  @Builder
  public RoadmapSemester(int year, int semester, SubjectStage stage, String reasoning) {
    this.year = year;
    this.semester = semester;
    this.stage = stage;
    this.reasoning = reasoning;
  }

  void assignTo(Roadmap owner) {
    this.roadmap = owner;
  }

  /** 학기 내 과목 항목을 추가하고 양방향 연관을 맞춘다. */
  public void addItem(RoadmapItem item) {
    items.add(item);
    item.assignTo(this);
  }
}
