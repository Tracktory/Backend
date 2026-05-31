package com.hansung.tracktory.domain.catalog.career.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기술 스택 — 직무가 요구하는 기술 단위. 이름이 유일하다. */
@Entity
@Table(name = "tech_stack")
@Getter
@NoArgsConstructor
public class TechStack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Builder
  public TechStack(String name) {
    this.name = name;
  }
}
