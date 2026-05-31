package com.hansung.tracktory.domain.catalog.classification.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 공부해 본/흥미 있는 개발 분야를 분류하는 코드 엔티티. */
@Entity
@Table(name = "dev_field")
@Getter
@NoArgsConstructor
public class DevField extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Builder
  public DevField(String code) {
    this.code = code;
  }
}
