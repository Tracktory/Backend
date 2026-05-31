package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserTrack;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTrackRepository extends JpaRepository<UserTrack, Long> {

  @EntityGraph(attributePaths = "track")
  List<UserTrack> findByUserIdOrderByTrackOrderAsc(Long userId);
}
