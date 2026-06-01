package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCompletedSubjectRepository extends JpaRepository<UserCompletedSubject, Long> {

  @EntityGraph(attributePaths = "subject")
  List<UserCompletedSubject> findByUserIdOrderByYearAscSemesterAsc(Long userId);

  boolean existsByUserIdAndSubjectId(Long userId, Long subjectId);

  long deleteByUserIdAndSubjectId(Long userId, Long subjectId);
}
