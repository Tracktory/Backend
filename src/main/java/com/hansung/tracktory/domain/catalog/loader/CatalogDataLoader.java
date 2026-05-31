package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.loader.CatalogRows.ClassificationData;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.CourseRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.JobRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TrackRow;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 클래스패스 기준 데이터를 읽어 카탈로그 테이블에 1회 적재한다. {@code catalog.seed.enabled=true} 일 때만 동작하며(기본 비활성), 평상시·테스트
 * 구동에는 영향이 없다. 운영자가 최초 적재 시점에만 플래그를 켠다. 각 seeder 가 멱등이라 재실행해도 중복 적재되지 않는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "catalog.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CatalogDataLoader implements ApplicationRunner {

  private final CatalogResourceReader resourceReader;
  private final ClassificationSeeder classificationSeeder;
  private final OrganizationSeeder organizationSeeder;
  private final CurriculumSeeder curriculumSeeder;
  private final CareerSeeder careerSeeder;

  @Override
  public void run(ApplicationArguments args) {
    log.info("카탈로그 기준 데이터 적재 시작");

    ClassificationData classifications =
        CatalogParser.parseClassifications(read("catalog/classifications.yaml"));
    classificationSeeder.seed(classifications);

    List<TrackRow> tracks = CatalogParser.parseTracks(read("catalog/tracks.yaml"));
    organizationSeeder.seed(tracks);

    List<CourseRow> courses = CatalogParser.parseCourses(read("catalog/courses.yaml"));
    curriculumSeeder.seed(courses);

    List<JobRow> jobs = CatalogParser.parseJobs(read("catalog/job_tech_stacks.json"));
    careerSeeder.seed(jobs);

    log.info(
        "카탈로그 기준 데이터 적재 완료 — 트랙 {}건, 과목 {}건, 직무 {}건", tracks.size(), courses.size(), jobs.size());
  }

  private Map<String, Object> read(String classpathLocation) {
    return resourceReader.readAsMap(classpathLocation);
  }
}
