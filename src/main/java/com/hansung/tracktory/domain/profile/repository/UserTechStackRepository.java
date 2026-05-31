package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserTechStack;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {

  @EntityGraph(attributePaths = "techStack")
  List<UserTechStack> findByUserIdOrderByIdAsc(Long userId);
}
