package com.hansung.tracktory.domain.catalog.career.repository;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTrack;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTrackRepository extends JpaRepository<JobTrack, Long> {
  boolean existsByJobAndTrack(Job job, Track track);
}
