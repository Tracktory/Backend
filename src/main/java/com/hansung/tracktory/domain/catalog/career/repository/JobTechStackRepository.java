package com.hansung.tracktory.domain.catalog.career.repository;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTechStack;
import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTechStackRepository extends JpaRepository<JobTechStack, Long> {
  boolean existsByJobAndTechStack(Job job, TechStack techStack);

  List<JobTechStack> findByJobIn(Collection<Job> jobs);
}
