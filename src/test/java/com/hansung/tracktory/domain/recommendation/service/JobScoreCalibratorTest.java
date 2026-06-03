package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobScoreCalibratorTest {

  @Test
  void toDisplayScore_appliesLinearFloorLiftAtRepresentativePoints() {
    assertThat(JobScoreCalibrator.toDisplayScore(0)).isEqualTo(60); // 하한 = 가장 낮은 점수도 60
    assertThat(JobScoreCalibrator.toDisplayScore(25)).isEqualTo(70);
    assertThat(JobScoreCalibrator.toDisplayScore(40)).isEqualTo(76);
    assertThat(JobScoreCalibrator.toDisplayScore(50)).isEqualTo(80);
    assertThat(JobScoreCalibrator.toDisplayScore(55)).isEqualTo(82); // 2250/100 = 22 → 82
    assertThat(JobScoreCalibrator.toDisplayScore(70)).isEqualTo(88);
    assertThat(JobScoreCalibrator.toDisplayScore(85)).isEqualTo(94);
    assertThat(JobScoreCalibrator.toDisplayScore(90)).isEqualTo(96);
    assertThat(JobScoreCalibrator.toDisplayScore(100)).isEqualTo(100); // 상한은 그대로
  }

  @Test
  void toDisplayScore_returnsNullForNullInput() {
    assertThat(JobScoreCalibrator.toDisplayScore(null)).isNull();
  }

  @Test
  void toDisplayScore_clampsOutOfRangeInput() {
    assertThat(JobScoreCalibrator.toDisplayScore(-5)).isEqualTo(60);
    assertThat(JobScoreCalibrator.toDisplayScore(150)).isEqualTo(100);
  }

  @Test
  void toDisplayScore_isMonotonicSoRankingIsPreserved() {
    int previous = JobScoreCalibrator.toDisplayScore(0);
    for (int internal = 1; internal <= 100; internal++) {
      int current = JobScoreCalibrator.toDisplayScore(internal);
      assertThat(current).isGreaterThanOrEqualTo(previous);
      previous = current;
    }
  }

  @Test
  void toDisplayScore_alwaysWithinFloorAndCeiling() {
    for (int internal = 0; internal <= 100; internal++) {
      assertThat(JobScoreCalibrator.toDisplayScore(internal)).isBetween(60, 100);
    }
  }
}
