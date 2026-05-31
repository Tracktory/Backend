package com.hansung.tracktory.domain.catalog.classification.repository;

import com.hansung.tracktory.domain.catalog.classification.entity.WorkValue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkValueRepository extends JpaRepository<WorkValue, Long> {
  Optional<WorkValue> findByCode(String code);

  boolean existsByCode(String code);
}
