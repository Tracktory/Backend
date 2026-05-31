package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserInterest;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

  @EntityGraph(attributePaths = "interest")
  List<UserInterest> findByUserIdOrderByIdAsc(Long userId);
}
