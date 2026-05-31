package com.hansung.tracktory.domain.catalog.classification.repository;

import com.hansung.tracktory.domain.catalog.classification.entity.DevField;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevFieldRepository extends JpaRepository<DevField, Long> {
  Optional<DevField> findByCode(String code);

  boolean existsByCode(String code);
}
