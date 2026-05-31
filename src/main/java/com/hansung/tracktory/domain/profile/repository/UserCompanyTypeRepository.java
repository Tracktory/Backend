package com.hansung.tracktory.domain.profile.repository;

import com.hansung.tracktory.domain.profile.entity.UserCompanyType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCompanyTypeRepository extends JpaRepository<UserCompanyType, Long> {

  @EntityGraph(attributePaths = "companyType")
  List<UserCompanyType> findByUserIdOrderByIdAsc(Long userId);
}
