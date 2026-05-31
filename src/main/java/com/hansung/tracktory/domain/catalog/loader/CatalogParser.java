package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.career.entity.JobTechStackStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.ClassificationData;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.CourseRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.JobRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TechStackRow;
import com.hansung.tracktory.domain.catalog.loader.CatalogRows.TrackRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 카탈로그 리소스의 파싱 결과 맵({@link CatalogResourceReader} 출력)을 중간 표현({@link CatalogRows})으로 변환한다. DB·Spring
 * 의존이 없는 순수 정적 함수라 단위 테스트로 매핑 규칙을 검증한다.
 *
 * <p>미지의 enum 후보값(알 수 없는 stage/course_type)은 조용히 흡수하지 않고 예외로 드러내, 기준 데이터 drift 를 적재 시점에 즉시 감지한다.
 */
public final class CatalogParser {

  private CatalogParser() {}

  /** tracks.yaml → 트랙 행 목록. code 는 AI 중계 서버 track_id 와 정합. */
  public static List<TrackRow> parseTracks(Map<String, Object> root) {
    return asMapList(root.get("tracks")).stream()
        .map(
            t ->
                new TrackRow(
                    asString(t.get("track_id")),
                    asString(t.get("college_id")),
                    asString(t.get("department_id")),
                    asString(t.get("track_name"))))
        .toList();
  }

  /** courses.yaml → 과목 행 목록. stage/course_type/available_semesters 를 enum 으로 매핑. */
  public static List<CourseRow> parseCourses(Map<String, Object> root) {
    return asMapList(root.get("courses")).stream()
        .map(
            c ->
                new CourseRow(
                    asString(c.get("course_id")),
                    asString(c.get("course_name")),
                    toCredit(c.get("credits")),
                    toSemester(asIntList(c.get("available_semesters"))),
                    toSubjectType(asString(c.get("course_type"))),
                    toSubjectStage(asString(c.get("stage"))),
                    asStringList(c.get("track_ids"))))
        .toList();
  }

  /** job_tech_stacks.json → 직무 행 목록. importance 를 학습 단계로 매핑. */
  public static List<JobRow> parseJobs(Map<String, Object> root) {
    return asMapList(root.get("job_categories")).stream()
        .map(
            cat -> {
              List<TechStackRow> techStacks =
                  asMapList(cat.get("tech_stacks")).stream()
                      .map(
                          s ->
                              new TechStackRow(
                                  asString(s.get("name")),
                                  toTechStage(asString(s.get("importance")))))
                      .toList();
              return new JobRow(
                  asString(cat.get("category_id")),
                  asString(cat.get("category_name_ko")),
                  asString(cat.get("category_name_en")),
                  techStacks);
            })
        .toList();
  }

  /** classifications.yaml → 온보딩 분류 taxonomy 전체. */
  public static ClassificationData parseClassifications(Map<String, Object> root) {
    return new ClassificationData(
        asStringList(root.get("interests")),
        asStringList(root.get("dev_fields")),
        asStringList(root.get("company_types")),
        asStringList(root.get("work_values")));
  }

  private static SubjectSemester toSemester(List<Integer> semesters) {
    boolean first = semesters.contains(1);
    boolean second = semesters.contains(2);
    if (first && second) {
      return SubjectSemester.BOTH;
    }
    if (first) {
      return SubjectSemester.FIRST;
    }
    if (second) {
      return SubjectSemester.SECOND;
    }
    return SubjectSemester.BOTH; // 미지정 시 양 학기 수강 가능으로 간주
  }

  private static SubjectType toSubjectType(String raw) {
    return switch (raw) {
      case "전공필수" -> SubjectType.REQUIRED;
      case "전공선택" -> SubjectType.ELECTIVE;
      default -> throw new IllegalStateException("알 수 없는 course_type: " + raw);
    };
  }

  private static SubjectStage toSubjectStage(String raw) {
    return switch (raw) {
      case "foundation" -> SubjectStage.FOUNDATION;
      case "core" -> SubjectStage.CORE;
      case "application" -> SubjectStage.APPLIED;
      case "industry" -> SubjectStage.INDUSTRY;
      default -> throw new IllegalStateException("알 수 없는 stage: " + raw);
    };
  }

  private static JobTechStackStage toTechStage(String importance) {
    return switch (importance) {
      case "필수" -> JobTechStackStage.CORE;
      case "권장", "우대" -> JobTechStackStage.BASIC;
      default -> throw new IllegalStateException("알 수 없는 importance: " + importance);
    };
  }

  private static BigDecimal toCredit(Object raw) {
    if (raw instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP);
    }
    return new BigDecimal(asString(raw)).setScale(1, RoundingMode.HALF_UP);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> asMapList(Object raw) {
    if (raw == null) {
      return List.of();
    }
    return (List<Map<String, Object>>) raw;
  }

  @SuppressWarnings("unchecked")
  private static List<String> asStringList(Object raw) {
    if (raw == null) {
      return List.of();
    }
    return ((List<Object>) raw).stream().map(CatalogParser::asString).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> asIntList(Object raw) {
    if (raw == null) {
      return List.of();
    }
    return ((List<Object>) raw).stream().map(o -> ((Number) o).intValue()).toList();
  }

  private static String asString(Object raw) {
    return raw == null ? null : raw.toString();
  }
}
