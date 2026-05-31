package com.hansung.tracktory.domain.recommendation.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * AI 중계 서버 추천 응답의 data 본문 — FastAPI RecommendResponse 와 그 중첩 모델을 mirror 한다. 모든 중첩 레코드는 snake_case
 * JSON 매핑 + 알 수 없는 필드 무시(meta_vector 등 미사용 필드)를 적용한다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRecommendResponse(
    List<JobCandidate> jobs,
    List<RankedCombo> primaryCombos,
    List<RankedCombo> secondaryCombos,
    Roadmap roadmap,
    Explanation explanation) {

  /** 추천 직무 단건. match_score 는 [0,1] 적합도. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record JobCandidate(
      String jobId,
      String jobName,
      List<String> techStacks,
      List<String> competencyTags,
      double matchScore,
      double similarity,
      boolean fallbackUsed) {}

  /** 트랙 단위. 매핑에 필요한 식별자만 사용하고 나머지(meta_vector 등)는 무시한다. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Track(String collegeId, String departmentId, String trackId, String trackName) {}

  /** 두 트랙의 조합. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TrackCombo(Track trackA, Track trackB, String comboKey) {}

  /** 시너지 점수·슬롯 분류·순위가 부착된 조합. slot_type: primary/cross_college/mmr. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RankedCombo(TrackCombo combo, double synergyScore, String slotType, int rank) {}

  /** 로드맵 한 단계 안의 추천 과목. score 는 [0,1] 과목 추천 점수, stage 는 학습 깊이 라벨. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RoadmapCourse(
      String courseId, String courseName, double score, int credits, String stage) {}

  /** 학습 깊이 단계 — foundation/core/application/industry. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RoadmapStage(String stage, List<RoadmapCourse> courses) {}

  /** 잔여 학기 한 학기 단위 추천 계획(미이수 미래 학기). */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SemesterPlan(
      int semester,
      int grade,
      List<RoadmapCourse> courses,
      int creditsTotal,
      boolean capReached,
      boolean graduationInsufficient) {}

  /** 로드맵 전체 — 학습 깊이 단계 + 미래 학기 분산 plan. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Roadmap(
      List<RoadmapStage> stages, List<SemesterPlan> semesters, String derivedFromComboKey) {}

  /** LLM 설명의 영역별 단락 — topic: jobs/tracks/roadmap. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ExplanationSection(String topic, String body) {}

  /** 학기 카드 헤더용 부제. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SemesterSubtitle(int semester, String subtitle) {}

  /** 과목 상세 모달용 인과 흐름. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CourseFlow(String courseId, String flow) {}

  /** LLM 자연어 설명 전체. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Explanation(
      String text,
      List<ExplanationSection> sections,
      List<SemesterSubtitle> semesterSubtitles,
      List<CourseFlow> courseFlows) {}
}
