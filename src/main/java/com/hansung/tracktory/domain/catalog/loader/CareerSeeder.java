package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTechStack;
import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import com.hansung.tracktory.domain.catalog.career.repository.JobRepository;
import com.hansung.tracktory.domain.catalog.career.repository.JobTechStackRepository;
import com.hansung.tracktory.domain.catalog.career.repository.TechStackRepository;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.JobRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TechStackRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 직무·기술 스택·직무↔기술 연결을 적재한다. 직무는 code, 기술은 이름, 연결은 (직무, 기술) 기준 멱등. */
@Service
@RequiredArgsConstructor
public class CareerSeeder {

  private final JobRepository jobRepository;
  private final TechStackRepository techStackRepository;
  private final JobTechStackRepository jobTechStackRepository;

  @Transactional
  public void seed(List<JobRow> jobs) {
    for (JobRow row : jobs) {
      Job job = resolveJob(row);
      for (TechStackRow stackRow : row.techStacks()) {
        TechStack techStack = resolveTechStack(stackRow.name());
        if (!jobTechStackRepository.existsByJobAndTechStack(job, techStack)) {
          jobTechStackRepository.save(
              JobTechStack.builder().stage(stackRow.stage()).job(job).techStack(techStack).build());
        }
      }
    }
  }

  private Job resolveJob(JobRow row) {
    return jobRepository
        .findByCode(row.code())
        .orElseGet(
            () ->
                jobRepository.save(
                    Job.builder()
                        .code(row.code())
                        .name(row.name())
                        .description(row.description())
                        .build()));
  }

  private TechStack resolveTechStack(String name) {
    return techStackRepository
        .findByName(name)
        .orElseGet(() -> techStackRepository.save(TechStack.builder().name(name).build()));
  }
}
