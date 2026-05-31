package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserDevField;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDevFieldRepository extends JpaRepository<UserDevField, Long> {

  @Modifying
  @Query("delete from UserDevField e where e.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  @EntityGraph(attributePaths = "devField")
  List<UserDevField> findByUserIdOrderByIdAsc(Long userId);
}
