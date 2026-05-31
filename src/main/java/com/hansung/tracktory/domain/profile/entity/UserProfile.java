package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 프로필 — 온보딩 결과의 루트. {@link User} 와 1:1 이며, 존재 자체가 온보딩 완료를 의미한다. */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_profile")
public class UserProfile extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @Column(name = "current_year", nullable = false)
  private Integer currentYear;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  // ------------------------------ 메서드 ------------------------------
  public static UserProfile of(User user, Department department, Integer currentYear, String name) {
    return UserProfile.builder()
        .user(user)
        .department(department)
        .currentYear(currentYear)
        .name(name)
        .build();
  }

  public void updateCurrentYear(Integer currentYear) {
    this.currentYear = currentYear;
  }

  public void updateName(String name) {
    this.name = name;
  }
}
