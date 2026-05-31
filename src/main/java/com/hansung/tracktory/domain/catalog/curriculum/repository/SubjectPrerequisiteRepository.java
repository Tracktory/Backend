package com.hansung.tracktory.domain.catalog.curriculum.repository;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectPrerequisite;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, Long> {
  boolean existsBySubjectAndPrerequisite(Subject subject, Subject prerequisite);

  List<SubjectPrerequisite> findBySubjectIn(Collection<Subject> subjects);
}
