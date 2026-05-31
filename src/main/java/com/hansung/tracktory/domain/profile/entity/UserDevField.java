package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 공부해본 개발 분야 (1~3개, 선택). */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_dev_field",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "dev_field_id"}))
public class UserDevField extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dev_field_id", nullable = false)
  private DevField devField;

  // ------------------------------ 메서드 ------------------------------
  public static UserDevField of(User user, DevField devField) {
    return UserDevField.builder().user(user).devField(devField).build();
  }
}
