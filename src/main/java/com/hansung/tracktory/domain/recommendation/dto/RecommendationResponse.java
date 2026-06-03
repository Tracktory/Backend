package com.hansung.tracktory.domain.recommendation.dto;

import java.util.List;

/**
 * 추천 생성 API 의 프론트엔드 응답 — 직무·트랙·로드맵 추천을 한 묶음으로 전달한다.
 *
 * <p>로드맵의 학기는 과거(이수 완료)·현재·미래(추천) 순으로 이어 붙여 제공하며, 각 과목은 시점·이수 여부·선수 과목을 함께 노출한다. 과거 학기는 AI 가 돌려주지
 * 않으므로 사용자의 이수 이력으로 재구성한 것이다.
 */
public record RecommendationResponse(
    Long recommendationId,
    List<JobView> jobs,
    TrackRecommendationView tracks,
    RoadmapView roadmap) {

  /**
   * 추천 직무 한 건. {@code score} 는 사용자 체감 척도로 보정한 표시 점수(0~100, 하한 위로 끌어올린 값이며 내부 저장 점수와는 다름), {@code
   * techStacks} 는 카탈로그가 보유한 직무 요구 기술 스택 이름 목록(없으면 빈 리스트).
   */
  public record JobView(
      String code, String name, Integer score, String reasoning, List<String> techStacks) {}

  /** 트랙 추천 — 주 추천 2개 + 보조 추천 다수, 최상위 조합의 시너지 요약을 포함한다. */
  public record TrackRecommendationView(
      Integer combinationScore,
      String combinationSummary,
      String combinationReasoning,
      List<TrackView> primary,
      List<TrackView> secondary) {}

  /**
   * 추천 트랙 한 건. {@code primary} 가 true 면 주 추천, {@code crossCombination} 이 true 면 학과 경계를 넘는 이색 조합(보조
   * 추천 중 별도 배지로 구분 노출 대상), {@code mainSubjects} 는 트랙의 주요 과목(전공필수) 목록.
   */
  public record TrackView(
      String code,
      String name,
      Integer score,
      String reasoning,
      boolean primary,
      boolean crossCombination,
      List<MainSubjectView> mainSubjects) {}

  /** 트랙의 주요 과목(전공필수) 한 건. */
  public record MainSubjectView(String code, String name) {}

  /** 학습 로드맵 전체 — 학기 그룹의 순서 리스트. */
  public record RoadmapView(String reasoning, List<SemesterView> semesters) {}

  /** 로드맵 학기 그룹. {@code stage} 는 기초/핵심/응용/산학 학습 단계. */
  public record SemesterView(
      int year, int semester, String stage, String timing, List<CourseView> courses) {}

  /**
   * 학기 내 과목. {@code timing} 은 PAST/CURRENT/FUTURE, {@code completed} 는 이수 여부, {@code score} 는 미래 추천
   * 과목의 적합도(0~100, 과거 과목은 null).
   */
  public record CourseView(
      String code,
      String name,
      String timing,
      boolean completed,
      Integer score,
      List<PrerequisiteView> prerequisites) {}

  /** 선수 과목 참조. */
  public record PrerequisiteView(String code, String name) {}
}
