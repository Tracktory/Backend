package com.hansung.tracktory.domain.catalog.curriculum.repository;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
  Optional<Subject> findByCode(String code);

  Optional<Subject> findFirstByNameOrderByIdAsc(String name);

  boolean existsByCode(String code);

  List<Subject> findByCodeIn(Collection<String> codes);
}
