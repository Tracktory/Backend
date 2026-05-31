package com.hansung.tracktory.domain.catalog.organization.repository;

import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
  Optional<Track> findByCode(String code);

  boolean existsByCode(String code);
}
