package com.hansung.tracktory.domain.catalog.curriculum.repository;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackSubjectRepository extends JpaRepository<TrackSubject, Long> {
  boolean existsByTrackAndSubject(Track track, Subject subject);
}
