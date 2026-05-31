package com.hansung.tracktory.domain.catalog.career.repository;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
  Optional<Job> findByCode(String code);

  boolean existsByCode(String code);
}
