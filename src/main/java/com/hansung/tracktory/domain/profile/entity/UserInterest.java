package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 관심 분야 (1~5개). */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_interest",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "interest_id"}))
public class UserInterest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  // ------------------------------ 메서드 ------------------------------
  public static UserInterest of(User user, Interest interest) {
    return UserInterest.builder().user(user).interest(interest).build();
  }
}
