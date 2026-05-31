package com.hansung.tracktory.domain.catalog.organization.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 단과대학 — 조직 계층의 최상위. AI 중계 서버는 단과대를 이름으로 식별한다. */
@Entity
@Table(name = "college")
@Getter
@NoArgsConstructor
public class College extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Builder
  public College(String name) {
    this.name = name;
  }
}
