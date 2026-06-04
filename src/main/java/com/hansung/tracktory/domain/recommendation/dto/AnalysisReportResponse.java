package com.hansung.tracktory.domain.recommendation.dto;

import java.util.List;

/**
 * 상세 분석 리포트 응답 — 활성 추천을 기준으로 기준 직무·역량 충족도·분야별 분석·잔여 과목·다음 액션·집계 수치를 한 묶음으로 전달한다.
 *
 * <p>역량 충족도(현재/예상)와 분야별 분석은 추천 생성 시점에 AI 가 산출해 보존한 스냅샷에서 나오고, 잔여 과목·이수 수·취득 학점은 카탈로그와 이수 이력으로 조회
 * 시점에 계산한다. 모두 같은 활성 추천을 기준으로 하므로 홈 추천 결과와 정합한다.
 *
 * <p>{@code anchorJob} 은 충족도(현재/다음 N개/전체)가 어느 직무를 기준으로 산출됐는지 알려주는 단일 기준 직무로, 화면의 "○○ 직무 기준" 라벨을
 * 채운다. 기본값은 매칭 1순위 직무이며, 조회 시 다른 추천 직무를 기준으로 지정하면 충족도가 그 직무 기준으로 재산출된다.
 */
public record AnalysisReportResponse(
    Long recommendationId,
    AnchorJobView anchorJob,
    CoverageView coverage,
    AggregateView aggregate,
    List<RemainingCourseView> remainingCourses,
    List<NextActionView> nextActions) {

  /** 충족도 산출 기준 직무 — "○○ 직무 기준" 라벨용. 추천 직무가 없으면 null. */
  public record AnchorJobView(String code, String name) {}

  /**
   * 역량 충족도 — 현재 → 다음 N개 이수 시 → 전체(천장) 3단 + 분야별 분석. 카운트는 {@code current ≤ nextActions ≤ expected} 를
   * 만족하고, 백분율은 {@code covered / required} 의 반올림이다(분모 0 이면 0). {@code nextActionsCovered} 는 다음 액션
   * 과목까지 이수 시 도달하는 합집합 충족 토큰 수로, 과목별 기여(+%)의 단순 합과 다르다.
   */
  public record CoverageView(
      int requiredCount,
      int currentCovered,
      int nextActionsCovered,
      int expectedCovered,
      int currentPercent,
      int nextActionsPercent,
      int expectedPercent,
      List<String> gapTokens,
      List<FieldCoverageView> fields) {}

  /** 분야(추천 직무)별 현재/예상 충족도. */
  public record FieldCoverageView(
      String jobCode,
      String jobName,
      int requiredCount,
      int currentCovered,
      int expectedCovered,
      int currentPercent,
      int expectedPercent,
      List<String> missingTokens) {}

  /** 이수 과목 수·취득 학점 집계. */
  public record AggregateView(int completedCourseCount, double earnedCredits) {}

  /**
   * 잔여(미이수) 과목 한 건. {@code type} 으로 전공필수/전공선택/전공기초를 구분해 필수 잔여만 추릴 수 있다. {@code tracks} 는 이 과목이 기여하는
   * 추천 트랙명 목록(한 과목이 여러 트랙에 걸칠 수 있음), {@code contributionPercent} 는 이 과목 단독 이수 시 역량 충족도 증가분(없으면
   * null).
   */
  public record RemainingCourseView(
      String code,
      String name,
      double credit,
      String type,
      String stage,
      List<String> tracks,
      Integer contributionPercent) {}

  /** 추천 기반 다음 액션 — 충족도를 가장 많이 올리는 과목 제안. */
  public record NextActionView(String code, String name, int contributionPercent, String message) {}
}
