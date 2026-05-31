package com.hansung.tracktory.domain.catalog.organization.entity;

import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 트랙 — 학부 내 전공 단위. {@code code} 로 AI 중계 서버의 track_id 와 정합한다. */
@Entity
@Table(
    name = "track",
    uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "name"}))
@Getter
@NoArgsConstructor
public class Track extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(nullable = false)
  private String name;

  @Builder
  public Track(String code, Department department, String name) {
    this.code = code;
    this.department = department;
    this.name = name;
  }
}
