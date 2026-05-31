package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTrackRepository extends JpaRepository<UserTrack, Long> {}
