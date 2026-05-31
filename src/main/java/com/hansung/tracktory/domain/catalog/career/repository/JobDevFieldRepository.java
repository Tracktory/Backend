package com.hansung.tracktory.domain.catalog.career.repository;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobDevField;
import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDevFieldRepository extends JpaRepository<JobDevField, Long> {
  boolean existsByJobAndDevField(Job job, DevField devField);
}
