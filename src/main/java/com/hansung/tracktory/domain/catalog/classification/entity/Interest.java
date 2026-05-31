package com.hansung.tracktory.domain.catalog.classification.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자의 관심 분야를 분류하는 코드 엔티티 — AI 중계 서버가 interest_id 로 식별한다. */
@Entity
@Table(name = "interest")
@Getter
@NoArgsConstructor
public class Interest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Builder
  public Interest(String code) {
    this.code = code;
  }
}
