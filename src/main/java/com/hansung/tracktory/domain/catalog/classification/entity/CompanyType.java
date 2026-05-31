package com.hansung.tracktory.domain.catalog.classification.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 희망하는 회사 유형을 분류하는 코드 엔티티. */
@Entity
@Table(name = "company_type")
@Getter
@NoArgsConstructor
public class CompanyType extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Builder
  public CompanyType(String code) {
    this.code = code;
  }
}
