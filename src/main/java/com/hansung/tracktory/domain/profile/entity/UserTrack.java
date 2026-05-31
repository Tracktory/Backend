package com.hansung.tracktory.domain.profile.entity;

import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 ↔ 트랙 — 1트랙/2트랙 구분은 {@code trackOrder} 로 표현한다. */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_track",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"user_id", "track_id"}),
      @UniqueConstraint(columnNames = {"user_id", "track_order"})
    })
public class UserTrack extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id", nullable = false)
  private Track track;

  @Column(name = "track_order", nullable = false)
  private Integer trackOrder;

  // ------------------------------ 메서드 ------------------------------
  public static UserTrack of(User user, Track track, Integer trackOrder) {
    return UserTrack.builder().user(user).track(track).trackOrder(trackOrder).build();
  }
}
