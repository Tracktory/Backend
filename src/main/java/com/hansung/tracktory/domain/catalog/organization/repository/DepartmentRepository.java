package com.hansung.tracktory.domain.catalog.organization.repository;

import com.hansung.tracktory.domain.catalog.organization.entity.College;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  Optional<Department> findByCollegeAndName(College college, String name);

  boolean existsByCollegeAndName(College college, String name);
}
