package com.hansung.tracktory.domain.catalog.curriculum.repository;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectJobRepository extends JpaRepository<SubjectJob, Long> {
  boolean existsBySubjectAndJob(Subject subject, Job job);
}
