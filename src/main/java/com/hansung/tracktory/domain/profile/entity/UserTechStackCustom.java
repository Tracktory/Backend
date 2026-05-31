package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 자유 입력한 기술 스택 (카탈로그에 없는 항목, 최대 40자). */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_tech_stack_custom",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "label"}))
public class UserTechStackCustom extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 40)
  private String label;

  // ------------------------------ 메서드 ------------------------------
  public static UserTechStackCustom of(User user, String label) {
    return UserTechStackCustom.builder().user(user).label(label).build();
  }
}
