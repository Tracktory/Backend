package com.hansung.tracktory.domain.recommendation.service;

/**
 * 직무 추천 점수를 응답에 노출하기 직전 사용자 체감 척도로 보정한다.
 *
 * <p>내부에 저장된 적합도 점수는 유사도 기반이라 절대값이 낮게 형성되어, 0~100 그대로 노출하면 실제 적합도에 비해 "추천이 부실하다"는 인상을 준다. 이 보정은 점수
 * 산출이나 저장값을 바꾸지 않고 <b>표시값만</b> 일정 하한 위로 끌어올려 신뢰도 인상을 개선한다.
 *
 * <p>규칙은 {@code 표시값 = 하한 + 내부값 × (100 − 하한) / 100} 의 선형 사상이다. 하한({@code DISPLAY_FLOOR})을 바닥으로 두어 가장
 * 낮은 점수도 일정 수준 이상으로 보이게 하되, 단조 증가라 직무 간 순위는 보존한다(더 높은 내부 점수는 항상 더 높은 표시값). 내부값 0→하한, 100→100 으로
 * 사상되어 출력은 항상 {@code [하한, 100]} 구간에 든다.
 */
final class JobScoreCalibrator {

  /** 표시 점수 하한 — 보정 강도를 조절하는 유일한 파라미터. 높일수록 낮은 점수가 더 끌어올려진다. */
  private static final int DISPLAY_FLOOR = 60;

  /** 내부·표시 점수의 공통 상한. */
  private static final int SCORE_MAX = 100;

  private JobScoreCalibrator() {
    // 유틸리티 클래스 — 인스턴스화 방지
  }

  /**
   * 내부 적합도 점수를 표시용 점수로 변환한다.
   *
   * @param internalScore 저장된 0~100 적합도(범위를 벗어나면 안전하게 clamp). {@code null} 이면 {@code null} 반환.
   * @return {@code [DISPLAY_FLOOR, 100]} 구간의 표시 점수(반올림). 입력이 {@code null} 이면 {@code null}.
   */
  static Integer toDisplayScore(Integer internalScore) {
    if (internalScore == null) {
      return null;
    }
    int clamped = Math.min(Math.max(internalScore, 0), SCORE_MAX);
    int span = SCORE_MAX - DISPLAY_FLOOR;
    // 정수 산술로 반올림(+ SCORE_MAX/2)하여 부동소수 drift 없이 결정적으로 계산한다.
    return DISPLAY_FLOOR + (clamped * span + SCORE_MAX / 2) / SCORE_MAX;
  }
}
