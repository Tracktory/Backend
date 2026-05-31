package com.hansung.tracktory.domain.catalog.classification.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 취업 시 중요하게 여기는 가치를 분류하는 코드 엔티티. */
@Entity
@Table(name = "work_value")
@Getter
@NoArgsConstructor
public class WorkValue extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Builder
  public WorkValue(String code) {
    this.code = code;
  }
}
