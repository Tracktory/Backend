package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserTechStackCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTechStackCustomRepository extends JpaRepository<UserTechStackCustom, Long> {

  @Modifying
  @Query("delete from UserTechStackCustom e where e.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
