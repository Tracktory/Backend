package com.hansung.tracktory.domain.catalog.career.repository;

import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
  Optional<TechStack> findByName(String name);

  boolean existsByName(String name);
}
