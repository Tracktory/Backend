package com.hansung.tracktory.domain.catalog.classification.repository;

import com.hansung.tracktory.domain.catalog.classification.entity.Interest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<Interest, Long> {
  Optional<Interest> findByCode(String code);

  boolean existsByCode(String code);
}
