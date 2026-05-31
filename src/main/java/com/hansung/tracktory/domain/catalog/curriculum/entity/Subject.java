package com.hansung.tracktory.domain.catalog.curriculum.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 교과목 — 커리큘럼의 기본 단위. {@code code} 로 AI 중계 서버의 course_id 와 정합한다. */
@Entity
@Table(name = "subject")
@Getter
@NoArgsConstructor
public class Subject extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false, precision = 3, scale = 1)
  private BigDecimal credit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectSemester semester;

  @Builder
  public Subject(
      String code, String name, String description, BigDecimal credit, SubjectSemester semester) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.credit = credit;
    this.semester = semester;
  }
}
