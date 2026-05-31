package com.hansung.tracktory.domain.catalog.classification.repository;

import com.hansung.tracktory.domain.catalog.classification.entity.CompanyType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyTypeRepository extends JpaRepository<CompanyType, Long> {
  Optional<CompanyType> findByCode(String code);

  boolean existsByCode(String code);
}
