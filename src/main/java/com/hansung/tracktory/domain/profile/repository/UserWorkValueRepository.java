package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserWorkValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWorkValueRepository extends JpaRepository<UserWorkValue, Long> {

  @Modifying
  @Query("delete from UserWorkValue e where e.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
