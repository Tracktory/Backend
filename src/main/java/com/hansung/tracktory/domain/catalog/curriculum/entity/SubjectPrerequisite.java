package com.hansung.tracktory.domain.catalog.curriculum.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과목 선수 관계 — 후수 과목({@code subject})이 요구하는 선수 과목({@code prerequisite})을 표현한다. */
@Entity
@Table(
    name = "subject_prerequisite",
    uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "prerequisite_id"}))
@Getter
@NoArgsConstructor
public class SubjectPrerequisite extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prerequisite_id", nullable = false)
  private Subject prerequisite;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PrerequisiteStrength strength;

  @Builder
  public SubjectPrerequisite(Subject subject, Subject prerequisite, PrerequisiteStrength strength) {
    this.subject = subject;
    this.prerequisite = prerequisite;
    this.strength = strength;
  }
}
