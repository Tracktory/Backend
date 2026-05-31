package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 공부해본 기술 스택 (카탈로그 선택). */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_tech_stack",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tech_stack_id"}))
public class UserTechStack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tech_stack_id", nullable = false)
  private TechStack techStack;

  // ------------------------------ 메서드 ------------------------------
  public static UserTechStack of(User user, TechStack techStack) {
    return UserTechStack.builder().user(user).techStack(techStack).build();
  }
}
