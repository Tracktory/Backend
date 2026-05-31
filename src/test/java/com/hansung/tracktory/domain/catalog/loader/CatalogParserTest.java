package com.hansung.tracktory.domain.catalog.loader;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.catalog.career.entity.JobTechStackStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.ClassificationData;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.CourseRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.JobRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TrackRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 클래스패스의 실제 기준 데이터를 읽어 파싱·매핑 규칙을 검증한다. Spring 컨텍스트·DB 없이 순수 단위 테스트. */
class CatalogParserTest {

  private final CatalogResourceReader reader = new CatalogResourceReader();

  @Test
  void parseTracks_트랙과_단과대_매핑() {
    List<TrackRow> tracks = CatalogParser.parseTracks(reader.readAsMap("catalog/tracks.yaml"));

    assertThat(tracks).hasSize(65);
    assertThat(tracks).allSatisfy(t -> assertThat(t.code()).isNotBlank());
    assertThat(tracks).extracting(TrackRow::collegeName).map(String::trim).contains("IT공과대학");
  }

  @Test
  void parseCourses_과목수와_enum_매핑() {
    List<CourseRow> courses = CatalogParser.parseCourses(reader.readAsMap("catalog/courses.yaml"));

    assertThat(courses).hasSize(954);
    assertThat(courses).allSatisfy(c -> assertThat(c.credit()).isNotNull());
    assertThat(courses)
        .extracting(CourseRow::stage)
        .containsAnyOf(SubjectStage.FOUNDATION, SubjectStage.CORE, SubjectStage.APPLIED);
    assertThat(courses)
        .extracting(CourseRow::type)
        .allMatch(t -> t == SubjectType.REQUIRED || t == SubjectType.ELECTIVE);
    assertThat(courses)
        .extracting(CourseRow::semester)
        .allMatch(
            s ->
                s == SubjectSemester.FIRST
                    || s == SubjectSemester.SECOND
                    || s == SubjectSemester.BOTH);
  }

  @Test
  void parseCourses_학점은_소수1자리_BigDecimal() {
    List<CourseRow> courses = CatalogParser.parseCourses(reader.readAsMap("catalog/courses.yaml"));

    CourseRow sample = courses.getFirst();
    assertThat(sample.credit().scale()).isEqualTo(1);
    assertThat(sample.credit()).isEqualByComparingTo(new BigDecimal("3.0"));
  }

  @Test
  void parseJobs_직무9개_각10개기술스택() {
    List<JobRow> jobs = CatalogParser.parseJobs(reader.readAsMap("catalog/job_tech_stacks.json"));

    assertThat(jobs).hasSize(9);
    assertThat(jobs).allSatisfy(j -> assertThat(j.techStacks()).hasSize(10));
    assertThat(jobs)
        .flatExtracting(JobRow::techStacks)
        .extracting(s -> s.stage())
        .contains(JobTechStackStage.CORE, JobTechStackStage.BASIC);

    JobRow frontend = jobs.stream().filter(j -> j.code().equals("FE")).findFirst().orElseThrow();
    assertThat(frontend.name()).isEqualTo("프론트엔드 개발자");
    assertThat(frontend.description()).isEqualTo("Frontend Developer");
  }

  @Test
  void parseClassifications_4종_taxonomy_개수() {
    ClassificationData data =
        CatalogParser.parseClassifications(reader.readAsMap("catalog/classifications.yaml"));

    assertThat(data.interests()).hasSize(14);
    assertThat(data.devFields()).hasSize(6);
    assertThat(data.companyTypes()).hasSize(7);
    assertThat(data.workValues()).hasSize(6);
  }
}
