package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 취업 가치관 (최대 3개). */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_work_value",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_value_id"}))
public class UserWorkValue extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "work_value_id", nullable = false)
  private WorkValue workValue;

  // ------------------------------ 메서드 ------------------------------
  public static UserWorkValue of(User user, WorkValue workValue) {
    return UserWorkValue.builder().user(user).workValue(workValue).build();
  }
}
