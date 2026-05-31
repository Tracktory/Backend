package com.hansung.tracktory.domain.catalog.career.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직무 카탈로그 항목 — AI 릴레이 서버와 code 컬럼으로 정렬되는 직무 단위를 표현한다. */
@Entity
@Table(name = "job")
@Getter
@NoArgsConstructor
public class Job extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Builder
  public Job(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }
}
