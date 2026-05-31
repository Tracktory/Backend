package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserDevField;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDevFieldRepository extends JpaRepository<UserDevField, Long> {

  @EntityGraph(attributePaths = "devField")
  List<UserDevField> findByUserIdOrderByIdAsc(Long userId);
}
