package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserWorkValue;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWorkValueRepository extends JpaRepository<UserWorkValue, Long> {

  @EntityGraph(attributePaths = "workValue")
  List<UserWorkValue> findByUserIdOrderByIdAsc(Long userId);
}
