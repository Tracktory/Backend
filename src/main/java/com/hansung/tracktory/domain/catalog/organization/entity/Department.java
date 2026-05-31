package com.hansung.tracktory.domain.catalog.organization.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학부 — 단과대학에 속하는 조직 단위. 같은 단과대 내에서 이름이 유일하다. */
@Entity
@Table(
    name = "department",
    uniqueConstraints = @UniqueConstraint(columnNames = {"college_id", "name"}))
@Getter
@NoArgsConstructor
public class Department extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "college_id", nullable = false)
  private College college;

  @Column(nullable = false)
  private String name;

  @Builder
  public Department(College college, String name) {
    this.college = college;
    this.name = name;
  }
}
