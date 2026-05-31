package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  /** 온보딩 완료 여부 판정 — 프로필 존재 자체가 완료를 의미한다. */
  boolean existsByUserId(Long userId);

  Optional<UserProfile> findByUserId(Long userId);
}
