package com.hansung.tracktory.domain.catalog.organization.repository;

import com.hansung.tracktory.domain.catalog.organization.entity.College;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
  Optional<College> findByName(String name);

  boolean existsByName(String name);
}
