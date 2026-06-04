package com.hansung.tracktory.domain.catalog.curriculum.repository;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackSubjectRepository extends JpaRepository<TrackSubject, Long> {
  boolean existsByTrackAndSubject(Track track, Subject subject);

  List<TrackSubject> findBySubjectIn(Collection<Subject> subjects);

  List<TrackSubject> findByTrackInAndType(Collection<Track> tracks, SubjectType type);

  List<TrackSubject> findByTrackIn(Collection<Track> tracks);
}
