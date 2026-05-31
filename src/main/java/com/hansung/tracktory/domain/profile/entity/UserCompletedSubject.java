package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 이수 과목 — 이수 학년/학기를 속성으로 가진다. */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_completed_subject",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "subject_id"}))
public class UserCompletedSubject extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  @Column(nullable = false)
  private Integer year;

  @Column(nullable = false)
  private Integer semester;

  // ------------------------------ 메서드 ------------------------------
  public static UserCompletedSubject of(
      User user, Subject subject, Integer year, Integer semester) {
    return UserCompletedSubject.builder()
        .user(user)
        .subject(subject)
        .year(year)
        .semester(semester)
        .build();
  }
}
