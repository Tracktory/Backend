package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TrackRow;
import com.hansung.tracktory.domain.catalog.organization.entity.College;
import com.hansung.tracktory.domain.catalog.organization.entity.Department;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.CollegeRepository;
import com.hansung.tracktory.domain.catalog.organization.repository.DepartmentRepository;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 조직 계층(단과대 → 학부 → 트랙)을 적재한다. 단과대·학부는 이름, 트랙은 code 기준 멱등. */
@Service
@RequiredArgsConstructor
public class OrganizationSeeder {

  private final CollegeRepository collegeRepository;
  private final DepartmentRepository departmentRepository;
  private final TrackRepository trackRepository;

  @Transactional
  public void seed(List<TrackRow> tracks) {
    Map<String, College> collegeCache = new HashMap<>();
    Map<String, Department> departmentCache = new HashMap<>();

    for (TrackRow row : tracks) {
      College college = resolveCollege(collegeCache, row.collegeName());
      Department department = resolveDepartment(departmentCache, college, row.departmentName());
      if (!trackRepository.existsByCode(row.code())) {
        trackRepository.save(
            Track.builder().code(row.code()).department(department).name(row.name()).build());
      }
    }
  }

  private College resolveCollege(Map<String, College> cache, String name) {
    return cache.computeIfAbsent(
        name,
        n ->
            collegeRepository
                .findByName(n)
                .orElseGet(() -> collegeRepository.save(College.builder().name(n).build())));
  }

  private Department resolveDepartment(
      Map<String, Department> cache, College college, String name) {
    String key = college.getName() + "\u0000" + name;
    return cache.computeIfAbsent(
        key,
        k ->
            departmentRepository
                .findByCollegeAndName(college, name)
                .orElseGet(
                    () ->
                        departmentRepository.save(
                            Department.builder().college(college).name(name).build())));
  }
}
